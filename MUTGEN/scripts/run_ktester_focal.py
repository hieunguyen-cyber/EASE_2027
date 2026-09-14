#!/usr/bin/env python3
"""Run a repository-level MUTGEN smoke experiment on one KTester focal method.

The ``prepare`` command is deliberately offline: it copies the selected project,
keeps the KTester test as a reference artifact, and creates an isolated empty
JUnit 5 test class for MUTGEN.  The ``run`` command performs the expensive work:
an initial PIT run, one MUTGEN generation pass, test validation, and a final PIT
run.  Mutation scores are calculated only for the selected focal method.
"""

from __future__ import annotations

import argparse
import contextlib
import datetime as dt
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import traceback
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET


MUTGEN_ROOT = Path(__file__).resolve().parents[1]
REPOSITORY_ROOT = MUTGEN_ROOT.parent
DEFAULT_DATASET = REPOSITORY_ROOT / "data/projects/puts/dataset_info.json"
DEFAULT_PROJECTS_ROOT = REPOSITORY_ROOT / "data/projects/puts"
DEFAULT_RUNS_ROOT = Path(
    os.getenv("MUTGEN_RUNS_ROOT")
    or REPOSITORY_ROOT / "runs/ktester_qwen25_coder_32b"
)
DEFAULT_TASK_ID = "PatternOptionBuilder_getValueType"
PITEST_VERSION = "1.18.0"
PITEST_JUNIT5_VERSION = "1.0.0"
SUREFIRE_VERSION = "3.2.5"


def utc_now() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat()


def read_json(path: Path) -> dict:
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    temporary.replace(path)


def find_task(dataset_path: Path, task_id: str) -> tuple[str, dict]:
    dataset = read_json(dataset_path)
    for project_key, project in dataset.items():
        for task in project.get("focal-methods", []):
            if task.get("id") == task_id:
                return project_key, task
    raise SystemExit(f"Không tìm thấy task '{task_id}' trong {dataset_path}")


def simple_test_class_name(task: dict) -> str:
    return task["test-class"].rsplit(".", 1)[-1]


def method_name(task: dict) -> str:
    return task["method-name"].split("(", 1)[0].strip()


def resolve_module_root(source_project: Path, task: dict) -> Path:
    """Find the Maven module against which task-relative paths are defined."""
    relative_source = Path(task["source-path"])
    if (source_project / relative_source).is_file():
        return source_project
    matches = []
    for candidate in source_project.rglob(relative_source.name):
        if not candidate.is_file():
            continue
        for parent in candidate.parents:
            if parent == source_project.parent:
                break
            if parent / relative_source == candidate:
                matches.append(parent)
                break
    unique = list(dict.fromkeys(matches))
    if len(unique) != 1:
        raise RuntimeError(
            f"Không resolve được đúng một Maven module cho {task['id']}: {unique}"
        )
    if not (unique[0] / "pom.xml").is_file():
        raise RuntimeError(f"Module của {task['id']} không có pom.xml: {unique[0]}")
    return unique[0]


def discover_reference_tests(module_root: Path, task: dict) -> list[Path]:
    exact = module_root / task["test-path"]
    if exact.is_file():
        return [exact]
    test_root = module_root / "src/test"
    focal_prefix = Path(task["test-path"]).stem.removesuffix("_Test")
    if not test_root.is_dir():
        return []
    candidates = sorted(test_root.rglob(f"{focal_prefix}*.java"))
    return [path for path in candidates if "_Test" in path.stem]


def required_java_major(project_name: str) -> int:
    # This KTester snapshot of commons-codec is configured for source/target 21.
    return 21 if project_name == "commons-codec" else 11


def install_reference_artifacts(
    reference_tests: list[Path], module_root: Path, reference_dir: Path
) -> list[str]:
    destination_root = reference_dir / "ktester_tests"
    copied = []
    for source in reference_tests:
        relative = source.relative_to(module_root)
        destination = destination_root / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)
        copied.append(str(destination))
    return copied


def add_pitest_plugin(pom_path: Path) -> None:
    namespace = "http://maven.apache.org/POM/4.0.0"
    ET.register_namespace("", namespace)
    ET.register_namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")
    tree = ET.parse(pom_path)
    project = tree.getroot()

    def q(name: str) -> str:
        return f"{{{namespace}}}{name}"

    build = project.find(q("build"))
    if build is None:
        build = ET.SubElement(project, q("build"))
    plugins = build.find(q("plugins"))
    if plugins is None:
        plugins = ET.SubElement(build, q("plugins"))

    # All generated tests are JUnit 5. Some KTester projects pin Surefire 2.x,
    # which silently discovers zero Jupiter tests.
    surefire = None
    for candidate in plugins.findall(q("plugin")):
        artifact = candidate.find(q("artifactId"))
        if artifact is not None and artifact.text == "maven-surefire-plugin":
            surefire = candidate
            break
    if surefire is None:
        surefire = ET.SubElement(plugins, q("plugin"))
        ET.SubElement(surefire, q("groupId")).text = "org.apache.maven.plugins"
        ET.SubElement(surefire, q("artifactId")).text = "maven-surefire-plugin"
    surefire_version = surefire.find(q("version"))
    if surefire_version is None:
        surefire_version = ET.SubElement(surefire, q("version"))
    surefire_version.text = SUREFIRE_VERSION

    # Gson's obfuscation-only test hooks require classes from its original test
    # suite, which is intentionally excluded from a focal-method experiment.
    for candidate in list(plugins.findall(q("plugin"))):
        artifact = candidate.find(q("artifactId"))
        if artifact is not None and artifact.text in {
            "copy-rename-maven-plugin",
            "proguard-maven-plugin",
        }:
            plugins.remove(candidate)

    properties = project.find(q("properties"))
    if properties is None:
        properties = ET.SubElement(project, q("properties"))
    arg_line = properties.find(q("argLine"))
    if arg_line is None:
        arg_line = ET.SubElement(properties, q("argLine"))
    arg_line.text = ""

    plugin = None
    for candidate in plugins.findall(q("plugin")):
        artifact = candidate.find(q("artifactId"))
        if artifact is not None and artifact.text == "pitest-maven":
            plugin = candidate
            break
    if plugin is None:
        plugin = ET.SubElement(plugins, q("plugin"))
        ET.SubElement(plugin, q("groupId")).text = "org.pitest"
        ET.SubElement(plugin, q("artifactId")).text = "pitest-maven"
    version = plugin.find(q("version"))
    if version is None:
        version = ET.SubElement(plugin, q("version"))
    version.text = PITEST_VERSION

    dependencies = plugin.find(q("dependencies"))
    if dependencies is None:
        dependencies = ET.SubElement(plugin, q("dependencies"))
    junit5_dependency = None
    for dependency in dependencies.findall(q("dependency")):
        artifact = dependency.find(q("artifactId"))
        if artifact is not None and artifact.text == "pitest-junit5-plugin":
            junit5_dependency = dependency
            break
    if junit5_dependency is None:
        junit5_dependency = ET.SubElement(dependencies, q("dependency"))
        ET.SubElement(junit5_dependency, q("groupId")).text = "org.pitest"
        ET.SubElement(junit5_dependency, q("artifactId")).text = (
            "pitest-junit5-plugin"
        )
    dependency_version = junit5_dependency.find(q("version"))
    if dependency_version is None:
        dependency_version = ET.SubElement(junit5_dependency, q("version"))
    dependency_version.text = PITEST_JUNIT5_VERSION

    tree.write(pom_path, encoding="UTF-8", xml_declaration=True)


def ensure_maven_wrapper(workspace: Path, projects_root: Path) -> None:
    """Reuse the wrapper already shipped by another KTester project."""
    destination = workspace / "mvnw"
    template_project = projects_root / "datafaker"
    wrapper = template_project / "mvnw"
    wrapper_config = template_project / ".mvn"
    if not destination.is_file():
        if not wrapper.is_file() or not wrapper_config.is_dir():
            return
        shutil.copy2(wrapper, destination)
    if not (workspace / ".mvn").is_dir() and wrapper_config.is_dir():
        shutil.copytree(wrapper_config, workspace / ".mvn")
    destination.chmod(destination.stat().st_mode | 0o111)
    cached_jar = MUTGEN_ROOT / ".tools/maven-wrapper-3.1.1.jar"
    target_jar = workspace / ".mvn/wrapper/maven-wrapper.jar"
    if cached_jar.is_file() and not target_jar.is_file():
        target_jar.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(cached_jar, target_jar)


def make_scaffold(task: dict) -> str:
    package = task["package"]
    class_name = simple_test_class_name(task)
    return f"""package {package};

import org.junit.jupiter.api.Test;

class {class_name} {{

    @Test
    void scaffoldLoads() {{
        // Keeps the test class discoverable before MUTGEN adds focal-method tests.
    }}
}}
"""


def command_text(args: list[str]) -> str:
    return " ".join(args)


def prepare(args: argparse.Namespace) -> int:
    dataset_path = Path(args.dataset).resolve()
    projects_root = Path(args.projects_root).resolve()
    runs_root = Path(args.runs_root).resolve()
    project_key, task = find_task(dataset_path, args.task_id)
    project_name = task.get("project-name", project_key)
    source_project = projects_root / project_name
    source_module = resolve_module_root(source_project, task)
    module_relative = source_module.relative_to(source_project)
    run_dir = runs_root / args.task_id
    workspace = run_dir / "workspace"
    build_dir = workspace / module_relative
    manifest_path = run_dir / "manifest.json"

    if manifest_path.exists():
        manifest = read_json(manifest_path)
        manifest["module_relative"] = str(module_relative)
        manifest["build_dir"] = str(build_dir)
        manifest["required_java_major"] = required_java_major(project_name)
        add_pitest_plugin(build_dir / "pom.xml")
        ensure_maven_wrapper(build_dir, projects_root)
        reference_dir = run_dir / "reference"
        reference_tests = discover_reference_tests(source_module, task)
        copied_references = install_reference_artifacts(
            reference_tests, source_module, reference_dir
        )
        manifest["isolation"]["ktester_tests"] = copied_references
        manifest["isolation"]["reference_status"] = (
            "available" if copied_references else "missing"
        )
        manifest["context_policy"] = {
                "model_sees": [
                    "KTester class-code context",
                    "KTester focal-method metadata",
                    "focal PIT mutation feedback",
                    "current generated-test scaffold",
                ],
                "build_uses": "complete copied Maven repository",
                "model_does_not_yet_see": "retrieved cross-file repository context",
                "ktester_tests_on_mutgen_classpath": False,
            }
        write_json(manifest_path, manifest)
        print(f"Đã chuẩn bị sẵn: {run_dir}")
        print(f"Trạng thái: {manifest.get('state', 'unknown')}")
        print(f"Chạy tối nay: {manifest['commands']['run']}")
        return 0
    if run_dir.exists():
        raise SystemExit(
            f"Thư mục run đã tồn tại nhưng không có manifest: {run_dir}. "
            "Hãy đổi tên thư mục này rồi chạy prepare lại."
        )
    if not (source_project / "pom.xml").is_file():
        raise SystemExit(f"Không tìm thấy Maven project: {source_project}")

    print(f"Đang copy project {project_name} sang workspace cô lập...")
    run_dir.mkdir(parents=True)
    shutil.copytree(
        source_project,
        workspace,
        ignore=shutil.ignore_patterns(".git", "target", ".idea", ".vscode"),
    )

    reference_dir = run_dir / "reference"
    reference_dir.mkdir()
    reference_tests = discover_reference_tests(source_module, task)
    copied_references = install_reference_artifacts(
        reference_tests, source_module, reference_dir
    )
    write_json(reference_dir / "task.json", task)

    copied_test_root = build_dir / "src/test"
    disabled_test_root = build_dir / "src/ktester-test-disabled"
    if copied_test_root.exists():
        copied_test_root.rename(disabled_test_root)
    generated_test = build_dir / task["test-path"]
    generated_test.parent.mkdir(parents=True, exist_ok=True)
    generated_test.write_text(make_scaffold(task), encoding="utf-8")
    add_pitest_plugin(build_dir / "pom.xml")
    ensure_maven_wrapper(build_dir, projects_root)

    relative_script = "scripts/run_ktester_focal.py"
    run_command = (
        f"python3 {relative_script} run --task-id {args.task_id}"
    )
    status_command = (
        f"python3 {relative_script} status --task-id {args.task_id}"
    )
    manifest = {
        "schema_version": 1,
        "state": "prepared",
        "prepared_at": utc_now(),
        "task_id": args.task_id,
        "project": project_name,
        "source_project": str(source_project),
        "workspace": str(workspace),
        "module_relative": str(module_relative),
        "build_dir": str(build_dir),
        "required_java_major": required_java_major(project_name),
        "dataset": str(dataset_path),
        "task": task,
        "isolation": {
            "ktester_tests": copied_references,
            "reference_status": "available" if copied_references else "missing",
            "disabled_project_tests": str(disabled_test_root),
            "mutgen_test": str(generated_test),
        },
        "pitest": {
            "version": PITEST_VERSION,
            "junit5_plugin_version": PITEST_JUNIT5_VERSION,
            "scope": {
                "target_class": task["class"],
                "target_test": task["test-class"],
                "scored_method": method_name(task),
            },
        },
        "context_policy": {
            "model_sees": [
                "KTester class-code context",
                "KTester focal-method metadata",
                "focal PIT mutation feedback",
                "current generated-test scaffold",
            ],
            "build_uses": "complete copied Maven repository",
            "model_does_not_yet_see": "retrieved cross-file repository context",
            "ktester_tests_on_mutgen_classpath": False,
        },
        "commands": {
            "run": run_command,
            "status": status_command,
        },
    }
    write_json(manifest_path, manifest)
    (run_dir / "RUN.md").write_text(
        "# One-focal-method MUTGEN run\n\n"
        f"Task: `{args.task_id}`\n\n"
        "From the MUTGEN directory, run:\n\n"
        f"```bash\nexport PYTHONPATH=\"$PWD\"\n{run_command}\n```\n\n"
        "The original KTester-generated test is preserved under `reference/` "
        "and is not present on MUTGEN's test classpath.\n",
        encoding="utf-8",
    )
    print(f"Chuẩn bị xong: {run_dir}")
    print(f"KTester reference files: {len(copied_references)}")
    print(f"MUTGEN scaffold: {generated_test}")
    print(f"Chạy tối nay: {run_command}")
    return 0


def parse_java_major(version_output: str) -> int | None:
    match = re.search(r'version\s+"(\d+)(?:\.(\d+))?', version_output)
    if not match:
        return None
    first = int(match.group(1))
    if first == 1 and match.group(2):
        return int(match.group(2))
    return first


def check_executable(name: str) -> str:
    path = shutil.which(name)
    if not path:
        raise RuntimeError(f"Không tìm thấy '{name}' trong PATH")
    return path


def configure_java(required_major: int = 11) -> tuple[str, str, int]:
    candidates = []
    candidates.extend(
        sorted(
            (MUTGEN_ROOT / ".tools").glob(f"jdk-{required_major}*/bin/java"),
            reverse=True,
        )
    )
    configured_home = os.getenv("JAVA_HOME")
    if configured_home:
        candidates.append(Path(configured_home) / "bin/java")
    candidates.extend(sorted((MUTGEN_ROOT / ".tools").glob("jdk-*/bin/java")))
    candidates.extend(
        [
            Path("/usr/lib/jvm/java-11-openjdk-amd64/bin/java"),
            Path("/usr/lib/jvm/java-17-openjdk-amd64/bin/java"),
        ]
    )
    path_java = shutil.which("java")
    if path_java:
        candidates.append(Path(path_java))

    checked = []
    for candidate in candidates:
        if not candidate.is_file():
            continue
        result = subprocess.run(
            [str(candidate), "-version"], capture_output=True, text=True, check=False
        )
        output = (result.stdout + result.stderr).strip()
        major = parse_java_major(output)
        checked.append(output.splitlines()[0] if output else str(candidate))
        if result.returncode == 0 and major is not None and major >= required_major:
            java_home = candidate.parent.parent.resolve()
            os.environ["JAVA_HOME"] = str(java_home)
            os.environ["PATH"] = str(java_home / "bin") + os.pathsep + os.environ["PATH"]
            return str(candidate.resolve()), output, major
    raise RuntimeError(
        f"Không tìm thấy Java >= {required_major}. Đã kiểm tra: "
        + "; ".join(checked)
    )


def find_maven(workspace: Path) -> str:
    installed = shutil.which("mvn")
    if installed:
        return installed
    wrapper = workspace / "mvnw"
    if wrapper.is_file() and os.access(wrapper, os.X_OK):
        return str(wrapper)
    raise RuntimeError("Không tìm thấy Maven hoặc Maven Wrapper")


def check_ollama(server_url: str, raw_model_name: str) -> None:
    request = urllib.request.Request(
        f"{server_url.rstrip('/')}/api/tags", headers={"Accept": "application/json"}
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            payload = json.load(response)
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as error:
        raise RuntimeError(
            f"Không kết nối được Ollama tại {server_url}: {error}"
        ) from error
    available = {item.get("name") for item in payload.get("models", [])}
    if raw_model_name not in available:
        raise RuntimeError(
            f"Model '{raw_model_name}' không có trên server. Available: "
            + ", ".join(sorted(name for name in available if name))
        )


def preflight(
    build_dir: Path, server_url: str, raw_model_name: str, required_major: int
) -> dict:
    java_path, java_output, java_major = configure_java(required_major)
    mvn_path = find_maven(build_dir)
    check_ollama(server_url, raw_model_name)
    return {
        "java": java_output.splitlines()[0],
        "java_major": java_major,
        "required_java_major": required_major,
        "java_path": java_path,
        "maven_path": mvn_path,
        "ollama_server": server_url,
        "ollama_model": raw_model_name,
    }


def run_command(
    command: list[str],
    cwd: Path,
    log_path: Path,
    label: str,
    check: bool = True,
) -> subprocess.CompletedProcess[str]:
    print(f"\n[{label}] {command_text(command)}")
    started = dt.datetime.now(dt.timezone.utc)
    timeout_seconds = int(os.getenv("MUTGEN_COMMAND_TIMEOUT_SECONDS", "1800"))
    try:
        result = subprocess.run(
            command,
            cwd=cwd,
            capture_output=True,
            text=True,
            timeout=timeout_seconds,
        )
    except subprocess.TimeoutExpired as error:
        log_path.parent.mkdir(parents=True, exist_ok=True)
        stdout = error.stdout or ""
        stderr = error.stderr or ""
        if isinstance(stdout, bytes):
            stdout = stdout.decode(errors="replace")
        if isinstance(stderr, bytes):
            stderr = stderr.decode(errors="replace")
        log_path.write_text(
            f"$ {command_text(command)}\n\n{stdout}\n[stderr]\n{stderr}",
            encoding="utf-8",
            errors="replace",
        )
        raise RuntimeError(
            f"Lệnh timeout sau {timeout_seconds}s ở bước {label}; log={log_path}"
        ) from error
    elapsed = (dt.datetime.now(dt.timezone.utc) - started).total_seconds()
    log_path.parent.mkdir(parents=True, exist_ok=True)
    log_path.write_text(
        f"$ {command_text(command)}\n\n"
        + result.stdout
        + ("\n[stderr]\n" + result.stderr if result.stderr else ""),
        encoding="utf-8",
        errors="replace",
    )
    print(f"[{label}] exit={result.returncode}, {elapsed:.1f}s, log={log_path}")
    if result.returncode != 0 and check:
        tail = (result.stdout + "\n" + result.stderr).splitlines()[-30:]
        raise RuntimeError(
            f"Lệnh thất bại ở bước {label}:\n" + "\n".join(tail)
        )
    return result


def pit_command(
    task: dict, maven: str = "mvn", target_tests: str | None = None
) -> list[str]:
    return [
        maven,
        "-q",
        "-Drat.skip=true",
        f"org.pitest:pitest-maven:{PITEST_VERSION}:mutationCoverage",
        f"-DtargetClasses={task['class']}",
        f"-DtargetTests={target_tests or task['test-class']}",
        "-Dmutators=DEFAULTS",
        "-DoutputFormats=XML,HTML",
        "-DtimestampedReports=false",
        "-DfailWhenNoMutations=false",
        "-Dthreads=1",
    ]


def newest_mutation_report(workspace: Path) -> Path:
    reports = list((workspace / "target/pit-reports").glob("**/mutations.xml"))
    if not reports:
        raise RuntimeError("PIT chạy xong nhưng không tìm thấy mutations.xml")
    return max(reports, key=lambda path: path.stat().st_mtime)


def element_text(node: ET.Element, name: str) -> str:
    value = node.findtext(name)
    return value.strip() if value else ""


def split_java_parameters(parameters: str) -> list[str]:
    values = []
    start = 0
    depth = 0
    for index, character in enumerate(parameters):
        if character in "<([":
            depth += 1
        elif character in ">)]":
            depth -= 1
        elif character == "," and depth == 0:
            values.append(parameters[start:index].strip())
            start = index + 1
    remainder = parameters[start:].strip()
    if remainder:
        values.append(remainder)
    return values


def normalize_java_type(value: str) -> str:
    return re.sub(r"\s+", "", value).replace("...", "[]")


def focal_line_range(source_path: Path, task: dict) -> tuple[int, int]:
    """Resolve the exact overload using class, parameter types, and the Java AST."""
    from tree_sitter_languages import get_parser

    signature = task["method-name"]
    expected_name = signature.split("(", 1)[0].strip()
    parameter_text = signature.rsplit(")", 1)[0].split("(", 1)[1]
    expected_types = split_java_parameters(parameter_text)
    expected_class = task["class"].rsplit(".", 1)[-1]
    parser = get_parser("java")
    tree = parser.parse(source_path.read_bytes())

    def class_nodes(node):
        if node.type in {
            "class_declaration",
            "interface_declaration",
            "enum_declaration",
            "record_declaration",
        }:
            yield node
        for child in node.children:
            yield from class_nodes(child)

    matches = []
    for class_node in class_nodes(tree.root_node):
        class_name = class_node.child_by_field_name("name")
        if class_name is None or class_name.text.decode() != expected_class:
            continue
        body = class_node.child_by_field_name("body")
        if body is None:
            continue
        for node in body.children:
            if node.type != "method_declaration":
                continue
            name = node.child_by_field_name("name")
            parameters = node.child_by_field_name("parameters")
            if name is None or name.text.decode() != expected_name or parameters is None:
                continue
            actual_types = []
            for parameter in parameters.children:
                if parameter.type not in {"formal_parameter", "spread_parameter"}:
                    continue
                type_node = parameter.child_by_field_name("type")
                actual_types.append(type_node.text.decode() if type_node else "")
            if len(actual_types) != len(expected_types):
                continue
            if all(
                normalize_java_type(actual) == normalize_java_type(expected)
                or normalize_java_type(actual).endswith(
                    "." + normalize_java_type(expected)
                )
                for actual, expected in zip(actual_types, expected_types)
            ):
                matches.append((node.start_point[0] + 1, node.end_point[0] + 1))
    if len(matches) != 1:
        raise RuntimeError(
            f"Không resolve được exact focal overload {task['class']}.{signature}: {matches}"
        )
    return matches[0]


def focal_mutations(
    report_path: Path, task: dict, source_path: Path | None = None
) -> list[dict]:
    root = ET.parse(report_path).getroot()
    selected = []
    focal_method = method_name(task)
    line_range = focal_line_range(source_path, task) if source_path else None
    for index, mutation in enumerate(root.findall(".//mutation"), start=1):
        if element_text(mutation, "mutatedClass") != task["class"]:
            continue
        if element_text(mutation, "mutatedMethod") != focal_method:
            continue
        line_number = int(element_text(mutation, "lineNumber") or 0)
        if line_range and not (line_range[0] <= line_number <= line_range[1]):
            continue
        status = mutation.attrib.get("status", "UNKNOWN")
        selected.append(
            {
                "id": index,
                "detected": mutation.attrib.get("detected", "false").lower()
                == "true",
                "status": status,
                "source_file": element_text(mutation, "sourceFile"),
                "line": line_number,
                "mutator": element_text(mutation, "mutator").rsplit(".", 1)[-1],
                "description": element_text(mutation, "description"),
                "method_description": element_text(mutation, "methodDescription"),
            }
        )
    return selected


def mutation_stats(mutations: list[dict]) -> dict:
    statuses: dict[str, int] = {}
    for mutation in mutations:
        status = mutation["status"]
        statuses[status] = statuses.get(status, 0) + 1
    total = len(mutations)
    detected = sum(1 for mutation in mutations if mutation["detected"])
    return {
        "total": total,
        "detected": detected,
        "undetected": total - detected,
        "killed": statuses.get("KILLED", 0),
        "survived": statuses.get("SURVIVED", 0),
        "no_coverage": statuses.get("NO_COVERAGE", 0),
        "mutation_score": round(detected / total, 6) if total else None,
        "statuses": dict(sorted(statuses.items())),
    }


def mutation_feedback(mutations: list[dict], source_path: Path, limit: int) -> str:
    source_lines = source_path.read_text(encoding="utf-8", errors="replace").splitlines()
    undetected = [mutation for mutation in mutations if not mutation["detected"]]
    selected = undetected[:limit]
    lines = [
        "PIT mutants for the focal method that the current scaffold did not kill.",
        "Generate assertions and inputs specifically capable of distinguishing these mutants.",
    ]
    for mutation in selected:
        line_number = mutation["line"]
        source = (
            source_lines[line_number - 1].strip()
            if 0 < line_number <= len(source_lines)
            else ""
        )
        lines.append(
            f"- status={mutation['status']}; line={line_number}; "
            f"mutator={mutation['mutator']}; description={mutation['description']}; "
            f"source={source}"
        )
    if len(undetected) > len(selected):
        lines.append(
            f"- Truncated {len(undetected) - len(selected)} additional undetected mutants."
        )
    if not undetected:
        lines.append("- No undetected focal mutants were reported.")
    return "\n".join(lines)


class Tee:
    def __init__(self, *streams):
        self.streams = streams

    def write(self, value: str) -> int:
        for stream in self.streams:
            stream.write(value)
            stream.flush()
        return len(value)

    def flush(self) -> None:
        for stream in self.streams:
            stream.flush()


@contextlib.contextmanager
def working_directory(path: Path):
    previous = Path.cwd()
    os.chdir(path)
    try:
        yield
    finally:
        os.chdir(previous)


def invoke_mutgen(
    workspace: Path,
    task: dict,
    feedback: str,
    model: str,
    server_url: str,
    maven: str,
    log_path: Path,
) -> str | None:
    sys.path.insert(0, str(MUTGEN_ROOT))
    from mutahunter.core.analyzer import Analyzer
    from mutahunter.core.coverage_processor import CoverageProcessor
    from mutahunter.core.entities.config import UnittestGeneratorLineConfig
    from mutahunter.core.prompt_factory import TestGenerationPromptFactory
    from mutahunter.core.router import LLMRouter
    from mutahunter.core.unit_test_gen import UnittestGenLine

    source_path = task["source-path"]
    test_path = task["test-path"]
    test_command = (
        f"{maven} -q -Drat.skip=true -Dtest={task['test-class']} test"
    )
    description = (
        "This is a repository-level KTester task. Use the KTester class context and "
        "the real Maven repository context. Generate tests only for the focal method "
        f"{task['class']}.{task['method-name']}.\n\nFocal method:\n"
        + task["focal-method"]
    )
    config = UnittestGeneratorLineConfig(
        model=model,
        api_base=server_url,
        test_file_path=test_path,
        source_file_path=source_path,
        test_command=test_command,
        code_coverage_report_path="target/site/jacoco/jacoco.xml",
        coverage_type="jacoco",
        target_line_coverage_rate=1.0,
        max_attempts=1,
        source_description=description,
        mutation_feedback=feedback,
        test_framework="JUnit 5",
        source_code_override=task.get("class-code", ""),
    )
    generator = UnittestGenLine(
        config=config,
        coverage_processor=CoverageProcessor(
            coverage_type="jacoco",
            code_coverage_report_path=config.code_coverage_report_path,
        ),
        analyzer=Analyzer(),
        router=LLMRouter(model=model, api_base=server_url),
        prompt=TestGenerationPromptFactory.get_prompt(),
    )
    error = None
    log_path.parent.mkdir(parents=True, exist_ok=True)
    with log_path.open("w", encoding="utf-8") as log_handle:
        tee_stdout = Tee(sys.stdout, log_handle)
        tee_stderr = Tee(sys.stderr, log_handle)
        try:
            with working_directory(workspace), contextlib.redirect_stdout(
                tee_stdout
            ), contextlib.redirect_stderr(tee_stderr):
                generator.run()
        except Exception:
            error = traceback.format_exc()
            log_handle.write("\n[MUTGEN ERROR]\n" + error)
    return error


def count_test_methods(path: Path) -> int:
    """Number of JUnit 5 test methods declared in a test source file."""
    if not path.is_file():
        return 0
    content = path.read_text(encoding="utf-8", errors="replace")
    return len(
        re.findall(r"@(?:Test|ParameterizedTest|RepeatedTest)\b", content)
    )


def java_test_class(path: Path) -> str:
    content = path.read_text(encoding="utf-8", errors="replace")
    package_match = re.search(r"^\s*package\s+([\w.]+)\s*;", content, re.MULTILINE)
    class_name = path.stem
    return (
        f"{package_match.group(1)}.{class_name}" if package_match else class_name
    )


def surefire_failures(
    build_dir: Path, test_classes: list[str]
) -> tuple[dict[str, set[str]], int]:
    failed_methods: dict[str, set[str]] = {}
    total_tests = 0
    report_dir = build_dir / "target/surefire-reports"
    for report in report_dir.glob("TEST-*.xml"):
        try:
            root = ET.parse(report).getroot()
        except ET.ParseError:
            continue
        class_name = root.attrib.get("name", "")
        if class_name not in test_classes:
            continue
        total_tests += int(root.attrib.get("tests", "0"))
        for test_case in root.findall("testcase"):
            if test_case.find("failure") is None and test_case.find("error") is None:
                continue
            name = test_case.attrib.get("name", "")
            # Parameterized reports commonly use method(arg)[index].
            method = re.split(r"[\[(]", name, maxsplit=1)[0]
            if method:
                failed_methods.setdefault(class_name, set()).add(method)
    return failed_methods, total_tests


def disable_test_methods(path: Path, method_names: set[str]) -> list[str]:
    if not method_names:
        return []
    from tree_sitter_languages import get_parser

    content = path.read_bytes()
    tree = get_parser("java").parse(content)
    insertions = []

    def walk(node):
        if node.type == "method_declaration":
            name = node.child_by_field_name("name")
            if name is not None and name.text.decode() in method_names:
                insertions.append((node.start_byte, name.text.decode()))
        for child in node.children:
            walk(child)

    walk(tree.root_node)
    disabled = []
    for offset, name in sorted(insertions, reverse=True):
        line_start = content.rfind(b"\n", 0, offset) + 1
        indentation = content[line_start:offset]
        annotation = (
            indentation
            + b'@org.junit.jupiter.api.Disabled("Fails in original KTester output")\n'
        )
        content = content[:line_start] + annotation + content[line_start:]
        disabled.append(name)
    path.write_bytes(content)
    return sorted(disabled)


def score_ktester_reference(
    run_dir: Path,
    build_dir: Path,
    manifest: dict,
    task: dict,
    maven: str,
    source_file: Path,
    logs_dir: Path,
    results_dir: Path,
) -> dict:
    references = [Path(path) for path in manifest["isolation"].get("ktester_tests", [])]
    if not references:
        return {"status": "REFERENCE_MISSING", "stats": None, "test_classes": []}

    reference_root = run_dir / "reference/ktester_tests"
    generated_test = build_dir / task["test-path"]
    saved_mutgen_test = results_dir / "mutgen_test.java"
    installed = []
    test_classes = []
    try:
        if generated_test.exists():
            generated_test.unlink()
        for reference in references:
            relative = reference.relative_to(reference_root)
            destination = build_dir / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            # Do not preserve the artifact timestamp: Maven must recompile when
            # the KTester class has the same FQN as the MutGen scaffold.
            shutil.copyfile(reference, destination)
            installed.append(destination)

        disabled_resources = (
            Path(manifest["isolation"]["disabled_project_tests"]) / "resources"
        )
        active_resources = build_dir / "src/test/resources"
        if disabled_resources.is_dir():
            shutil.copytree(disabled_resources, active_resources, dirs_exist_ok=True)

        test_classes = [java_test_class(path) for path in installed]
        selector = ",".join(test_classes)
        raw_test_result = run_command(
            [
                maven,
                "-q",
                "-Drat.skip=true",
                f"-Dtest={selector}",
                "clean",
                "test",
            ],
            build_dir,
            logs_dir / "07_ktester_test.log",
            "ktester-test",
            check=False,
        )
        disabled_methods = []
        total_tests = 0
        reference_status = "SCORED"
        if raw_test_result.returncode != 0:
            failures_by_class, total_tests = surefire_failures(build_dir, test_classes)
            if not failures_by_class:
                tail = (raw_test_result.stdout + "\n" + raw_test_result.stderr).splitlines()[-30:]
                raise RuntimeError(
                    "KTester reference không compile hoặc không tạo Surefire report:\n"
                    + "\n".join(tail)
                )
            for path in installed:
                class_name = java_test_class(path)
                disabled_methods.extend(
                    disable_test_methods(path, failures_by_class.get(class_name, set()))
                )
            disabled_methods = sorted(set(disabled_methods))
            run_command(
                [
                    maven,
                    "-q",
                    "-Drat.skip=true",
                    f"-Dtest={selector}",
                    "test",
                ],
                build_dir,
                logs_dir / "07b_ktester_filtered_test.log",
                "ktester-filtered-test",
            )
            reference_status = "SCORED_FILTERED"
        else:
            _, total_tests = surefire_failures(build_dir, test_classes)
        run_command(
            pit_command(task, maven, selector),
            build_dir,
            logs_dir / "08_ktester_pit.log",
            "ktester-pit",
        )
        report = newest_mutation_report(build_dir)
        report_copy = results_dir / "ktester_mutations.xml"
        shutil.copy2(report, report_copy)
        mutants = focal_mutations(report_copy, task, source_file)
        stats = mutation_stats(mutants)
        write_json(
            results_dir / "ktester_focal_mutants.json", {"mutants": mutants}
        )
        return {
            "status": reference_status,
            "stats": stats,
            "test_classes": test_classes,
            "reference_files": [str(path) for path in references],
            "raw_test_count": total_tests,
            "disabled_failing_methods": disabled_methods,
        }
    except Exception as error:
        message = str(error)
        uncompilable = (
            "không compile" in message
            and ("illegal start of expression" in message
                 or "cannot find symbol" in message
                 or "not a statement" in message
                 or "COMPILATION ERROR" in message)
        )
        return {
            "status": "REFERENCE_UNCOMPILABLE" if uncompilable else "FAILED",
            "stats": None,
            "test_classes": test_classes,
            "reference_files": [str(path) for path in references],
            "error": message,
        }
    finally:
        for path in installed:
            if path.exists():
                path.unlink()
        if saved_mutgen_test.is_file():
            generated_test.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(saved_mutgen_test, generated_test)


def run(args: argparse.Namespace) -> int:
    run_dir = Path(args.runs_root).resolve() / args.task_id
    manifest_path = run_dir / "manifest.json"
    if not manifest_path.exists():
        raise SystemExit(
            "Chưa có workspace. Chạy prepare trước: "
            f"python3 scripts/run_ktester_focal.py prepare --task-id {args.task_id}"
        )
    manifest = read_json(manifest_path)
    if manifest.get("state") == "completed" and not args.allow_rerun:
        raise SystemExit(
            "Run này đã completed. Dùng một run directory mới để giữ tính tái lập; "
            "hoặc truyền --allow-rerun nếu bạn chủ động muốn chạy trên test hiện tại."
        )
    workspace = Path(manifest["workspace"])
    build_dir = Path(manifest.get("build_dir", manifest["workspace"]))
    task = manifest["task"]
    results_dir = run_dir / "results"
    logs_dir = run_dir / "logs"
    results_dir.mkdir(exist_ok=True)
    logs_dir.mkdir(exist_ok=True)

    server_url = os.getenv(
        "OLLAMA_SERVER_URL", "http://10.148.182.141:11434"
    ).rstrip("/")
    raw_model = os.getenv("OLLAMA_MODEL", "qwen2.5-coder:32b")
    model = os.getenv("MUTGEN_MODEL", f"ollama/{raw_model}")
    print("Kiểm tra Java, Maven và Ollama...")
    environment = preflight(
        build_dir,
        server_url,
        raw_model,
        int(manifest.get("required_java_major", 11)),
    )
    maven = environment["maven_path"]
    manifest.update(
        {
            "state": "running",
            "started_at": utc_now(),
            "environment": environment,
            "model": model,
        }
    )
    write_json(manifest_path, manifest)

    try:
        run_command(
            [maven, "-q", "-Drat.skip=true", "-DskipTests", "compile"],
            build_dir,
            logs_dir / "01_compile.log",
            "compile",
        )
        run_command(
            [
                maven,
                "-q",
                "-Drat.skip=true",
                f"-Dtest={task['test-class']}",
                "test",
            ],
            build_dir,
            logs_dir / "02_scaffold_test.log",
            "scaffold-test",
        )
        run_command(
            pit_command(task, maven),
            build_dir,
            logs_dir / "03_initial_pit.log",
            "initial-pit",
        )
        initial_report = newest_mutation_report(build_dir)
        initial_copy = results_dir / "initial_mutations.xml"
        shutil.copy2(initial_report, initial_copy)
        source_file = build_dir / task["source-path"]
        initial_mutants = focal_mutations(initial_copy, task, source_file)
        initial_stats = mutation_stats(initial_mutants)
        write_json(
            results_dir / "initial_focal_mutants.json",
            {"mutants": initial_mutants},
        )
        if not initial_mutants:
            raise RuntimeError(
                "PIT không tạo mutant nào cho focal method; kiểm tra tên/signature trong task."
            )
        feedback = mutation_feedback(
            initial_mutants,
            source_file,
            args.max_feedback_mutants,
        )
        (results_dir / "mutation_feedback.txt").write_text(
            feedback + "\n", encoding="utf-8"
        )
        print(
            "Initial focal score: "
            f"{initial_stats['detected']}/{initial_stats['total']} "
            f"({initial_stats['mutation_score']:.3f})"
        )

        print("\n[MUTGEN] Gọi model và validate từng test sinh ra...")
        generation_error = invoke_mutgen(
            build_dir,
            task,
            feedback,
            model,
            server_url,
            maven,
            logs_dir / "04_mutgen.log",
        )
        generated_test = build_dir / task["test-path"]
        shutil.copy2(generated_test, results_dir / "mutgen_test.java")
        if generation_error:
            print("MUTGEN có lỗi; vẫn chạy PIT cuối để lưu observation. Xem 04_mutgen.log")

        final_test_result = run_command(
            [
                maven,
                "-q",
                "-Drat.skip=true",
                f"-Dtest={task['test-class']}",
                "test",
            ],
            build_dir,
            logs_dir / "05_final_test.log",
            "final-test",
            check=False,
        )
        mutgen_failures, mutgen_executed = surefire_failures(
            build_dir, [task["test-class"]]
        )
        mutgen_failing = sorted(
            method
            for methods in mutgen_failures.values()
            for method in methods
        )
        mutgen_test_stats = {
            "declared_test_methods": count_test_methods(
                results_dir / "mutgen_test.java"
            ),
            "executed_tests": mutgen_executed,
            "failing_tests": len(mutgen_failing),
            "passing_tests": max(mutgen_executed - len(mutgen_failing), 0),
            "suite_green": final_test_result.returncode == 0,
            "failing_test_methods": mutgen_failing,
        }
        if final_test_result.returncode != 0:
            print(
                "[final-test] Suite MUTGEN không xanh "
                f"({len(mutgen_failing)} test lỗi); vẫn chạy PIT để lưu observation."
            )
        run_command(
            pit_command(task, maven),
            build_dir,
            logs_dir / "06_final_pit.log",
            "final-pit",
        )
        final_report = newest_mutation_report(build_dir)
        final_copy = results_dir / "final_mutations.xml"
        shutil.copy2(final_report, final_copy)
        final_mutants = focal_mutations(final_copy, task, source_file)
        final_stats = mutation_stats(final_mutants)
        write_json(
            results_dir / "final_focal_mutants.json",
            {"mutants": final_mutants},
        )
        if args.skip_ktester_score:
            ktester_result = {
                "status": "SKIPPED",
                "stats": None,
                "test_classes": [],
            }
        else:
            print("\n[KTESTER] Chạy reference tests với cùng cấu hình PIT...")
            ktester_result = score_ktester_reference(
                run_dir,
                build_dir,
                manifest,
                task,
                maven,
                source_file,
                logs_dir,
                results_dir,
            )
        ktester_score = (
            ktester_result["stats"]["mutation_score"]
            if ktester_result.get("stats")
            else None
        )
        summary = {
            "task_id": args.task_id,
            "project": manifest["project"],
            "focal_class": task["class"],
            "focal_method": task["method-name"],
            "model": model,
            "server": server_url,
            "context_policy": manifest["context_policy"],
            "started_at": manifest["started_at"],
            "completed_at": utc_now(),
            "generation_error": generation_error,
            "initial": initial_stats,
            "final": final_stats,
            "mutgen_tests": mutgen_test_stats,
            "ktester": ktester_result,
            "score_delta": (
                round(final_stats["mutation_score"] - initial_stats["mutation_score"], 6)
                if final_stats["mutation_score"] is not None
                and initial_stats["mutation_score"] is not None
                else None
            ),
            "mutgen_minus_ktester": (
                round(final_stats["mutation_score"] - ktester_score, 6)
                if final_stats["mutation_score"] is not None
                and ktester_score is not None
                else None
            ),
            "artifacts": {
                "ktester_references": manifest["isolation"].get(
                    "ktester_tests", []
                ),
                "mutgen_test": str(results_dir / "mutgen_test.java"),
                "initial_pit_xml": str(initial_copy),
                "final_pit_xml": str(final_copy),
                "mutation_feedback": str(results_dir / "mutation_feedback.txt"),
            },
        }
        write_json(results_dir / "summary.json", summary)
        manifest.update(
            {
                "state": "completed",
                "completed_at": summary["completed_at"],
                "summary": str(results_dir / "summary.json"),
            }
        )
        write_json(manifest_path, manifest)
        print(
            "\nHoàn tất. Final focal score: "
            f"{final_stats['detected']}/{final_stats['total']} "
            f"({final_stats['mutation_score']:.3f}); "
            f"delta={summary['score_delta']:+.3f}"
        )
        print(f"Summary: {results_dir / 'summary.json'}")
        return 0
    except Exception as error:
        manifest.update(
            {
                "state": "failed",
                "failed_at": utc_now(),
                "error": str(error),
            }
        )
        write_json(manifest_path, manifest)
        raise


def status(args: argparse.Namespace) -> int:
    run_dir = Path(args.runs_root).resolve() / args.task_id
    manifest_path = run_dir / "manifest.json"
    if not manifest_path.exists():
        print(f"Chưa prepare: {run_dir}")
        return 1
    manifest = read_json(manifest_path)
    print(f"Task: {manifest['task_id']}")
    print(f"Project: {manifest['project']}")
    print(f"State: {manifest['state']}")
    print(f"Workspace: {manifest['workspace']}")
    if manifest.get("error"):
        print(f"Error: {manifest['error']}")
    summary_path = manifest.get("summary")
    if summary_path and Path(summary_path).exists():
        summary = read_json(Path(summary_path))
        print(f"Initial: {summary['initial']}")
        print(f"Final: {summary['final']}")
        print(f"KTester: {summary.get('ktester')}")
        print(f"Delta: {summary['score_delta']}")
    else:
        print(f"Run command: {manifest['commands']['run']}")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Repository-level MUTGEN experiment for one KTester focal method."
    )
    common = argparse.ArgumentParser(add_help=False)
    common.add_argument("--task-id", default=DEFAULT_TASK_ID)
    common.add_argument("--runs-root", default=str(DEFAULT_RUNS_ROOT))
    subparsers = parser.add_subparsers(dest="command", required=True)

    prepare_parser = subparsers.add_parser("prepare", parents=[common])
    prepare_parser.add_argument("--dataset", default=str(DEFAULT_DATASET))
    prepare_parser.add_argument("--projects-root", default=str(DEFAULT_PROJECTS_ROOT))
    prepare_parser.set_defaults(handler=prepare)

    run_parser = subparsers.add_parser("run", parents=[common])
    run_parser.add_argument("--max-feedback-mutants", type=int, default=80)
    run_parser.add_argument("--allow-rerun", action="store_true")
    run_parser.add_argument(
        "--skip-ktester-score",
        action="store_true",
        help="Skip scoring the original KTester reference test(s).",
    )
    run_parser.set_defaults(handler=run)

    status_parser = subparsers.add_parser("status", parents=[common])
    status_parser.set_defaults(handler=status)
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    try:
        return args.handler(args)
    except (RuntimeError, OSError, ET.ParseError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
