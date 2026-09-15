"""The only module that reaches into the MUTGEN checkout.

`scripts/run_ktester_focal.py` already solved the awkward repository-level
plumbing for this dataset -- Maven module resolution, the PIT plugin injection,
focal-method line ranges, Surefire parsing, JDK selection per project.  It is
import-safe (module-level constants only, `main()` behind a guard), so the
fusion reuses those helpers instead of forking them and letting the two copies
drift apart.

Everything MUTGEN-specific that the fusion needs is re-exported from here.
"""

from __future__ import annotations

import importlib.util
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

import config


def _load_mutgen_script(filename: str, alias: str):
    """Import one of MUTGEN's scripts as a module, not as a subprocess."""
    script = config.MUTGEN_ROOT / "scripts" / filename
    if not script.is_file():
        raise SystemExit(
            f"Không tìm thấy script của MUTGEN: {script}\n"
            "Đặt FUSE_MUTGEN_ROOT nếu checkout MUTGEN nằm chỗ khác."
        )
    # mutahunter is imported lazily by the runner; make sure it resolves.
    for path in (config.MUTGEN_ROOT, config.MUTGEN_ROOT / "scripts"):
        if str(path) not in sys.path:
            sys.path.insert(0, str(path))
    spec = importlib.util.spec_from_file_location(alias, script)
    module = importlib.util.module_from_spec(spec)
    sys.modules[alias] = module
    spec.loader.exec_module(module)
    return module


focal = _load_mutgen_script("run_ktester_focal.py", "mutgen_focal_runner")
# The KTester paper-metric protocol (CPR/EPR/LC/BC and the published baseline)
# is already encoded here; reuse it so both studies report the same numbers.
paper = _load_mutgen_script(
    "evaluate_ktester_paper_metrics.py", "mutgen_paper_metrics"
)

# --- dataset / workspace -------------------------------------------------
find_task = focal.find_task
resolve_module_root = focal.resolve_module_root
required_java_major = focal.required_java_major


def add_pitest_plugin(pom_path: Path) -> None:
    """Namespace-aware replacement for MUTGEN's version.

    MUTGEN hardcodes the `{http://maven.apache.org/POM/4.0.0}` prefix in every
    lookup. jdom2's pom declares no namespace at all (`<project>` bare), so
    every find() misses and the function appends a *second* <build> and
    <properties> next to the existing ones -- Maven then rejects the file with
    "Duplicated tag: 'properties'" and all 21 jdom2 tasks die before compiling.
    Reading the namespace off the root element fixes both shapes.
    """
    tree = ET.parse(pom_path)
    project = tree.getroot()
    namespace = (
        project.tag.partition("}")[0].removeprefix("{")
        if project.tag.startswith("{")
        else ""
    )

    def q(name: str) -> str:
        return f"{{{namespace}}}{name}" if namespace else name

    def child(parent, name):
        found = parent.find(q(name))
        return found if found is not None else ET.SubElement(parent, q(name))

    def named_plugin(plugins, artifact_id):
        for candidate in plugins.findall(q("plugin")):
            artifact = candidate.find(q("artifactId"))
            if artifact is not None and artifact.text == artifact_id:
                return candidate
        return None

    plugins = child(child(project, "build"), "plugins")

    # All generated tests are JUnit 5; some subjects pin Surefire 2.x, which
    # silently discovers zero Jupiter tests.
    surefire = named_plugin(plugins, "maven-surefire-plugin")
    if surefire is None:
        surefire = ET.SubElement(plugins, q("plugin"))
        ET.SubElement(surefire, q("groupId")).text = "org.apache.maven.plugins"
        ET.SubElement(surefire, q("artifactId")).text = "maven-surefire-plugin"
    child(surefire, "version").text = focal.SUREFIRE_VERSION

    # Gson's obfuscation-only hooks need its original test suite, which a
    # focal-method experiment deliberately excludes.
    for artifact_id in ("copy-rename-maven-plugin", "proguard-maven-plugin"):
        stale = named_plugin(plugins, artifact_id)
        if stale is not None:
            plugins.remove(stale)

    child(child(project, "properties"), "argLine").text = ""

    pitest = named_plugin(plugins, "pitest-maven")
    if pitest is None:
        pitest = ET.SubElement(plugins, q("plugin"))
        ET.SubElement(pitest, q("groupId")).text = "org.pitest"
        ET.SubElement(pitest, q("artifactId")).text = "pitest-maven"
    child(pitest, "version").text = focal.PITEST_VERSION

    dependencies = child(pitest, "dependencies")
    junit5 = None
    for dependency in dependencies.findall(q("dependency")):
        artifact = dependency.find(q("artifactId"))
        if artifact is not None and artifact.text == "pitest-junit5-plugin":
            junit5 = dependency
            break
    if junit5 is None:
        junit5 = ET.SubElement(dependencies, q("dependency"))
        ET.SubElement(junit5, q("groupId")).text = "org.pitest"
        ET.SubElement(junit5, q("artifactId")).text = "pitest-junit5-plugin"
    child(junit5, "version").text = focal.PITEST_JUNIT5_VERSION

    if namespace:
        ET.register_namespace("", namespace)
        ET.register_namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")
    tree.write(pom_path, encoding="UTF-8", xml_declaration=True)

ensure_maven_wrapper = focal.ensure_maven_wrapper
simple_test_class_name = focal.simple_test_class_name
method_name = focal.method_name
discover_reference_tests = focal.discover_reference_tests

# --- environment ---------------------------------------------------------
configure_java = focal.configure_java
find_maven = focal.find_maven
check_ollama = focal.check_ollama
run_command = focal.run_command

# --- Build-quality gates ------------------------------------------------
# Some subjects fail the build on formatting/static-analysis before a single
# test runs -- datafaker's spotless:check rejects the KTester test file over
# CRLF line endings and layout. None of these gates affect coverage or mutation
# measurement, so they are skipped everywhere rather than losing the subject.
SKIP_FLAGS = [
    "-Drat.skip=true",
    "-Dspotless.check.skip=true",
    "-Dspotless.apply.skip=true",
    "-Dcheckstyle.skip=true",
    "-Dpmd.skip=true",
    "-Dcpd.skip=true",
    "-Dspotbugs.skip=true",
    "-Denforcer.skip=true",
    "-Danimal.sniffer.skip=true",
    "-Dmaven.javadoc.skip=true",
]


# --- PIT -----------------------------------------------------------------
def pit_command(task: dict, maven: str = "mvn", target_tests: str | None = None) -> list[str]:
    """MUTGEN's PIT invocation, with the quality gates skipped."""
    command = focal.pit_command(task, maven, target_tests)
    return command[:2] + SKIP_FLAGS + [
        part for part in command[2:] if part != "-Drat.skip=true"
    ]

newest_mutation_report = focal.newest_mutation_report
focal_mutations = focal.focal_mutations
mutation_stats = focal.mutation_stats
focal_line_range = focal.focal_line_range

# --- JUnit test-file inspection -----------------------------------------
count_test_methods = focal.count_test_methods
java_test_class = focal.java_test_class
surefire_failures = focal.surefire_failures
disable_test_methods = focal.disable_test_methods

# --- KTester paper protocol (re-exported from MUTGEN's adapter) ----------
PAPER_KTESTER = paper.PAPER_KTESTER
JACOCO_VERSION = paper.JACOCO_VERSION
enable_jacoco_argline = paper.enable_jacoco_argline
parse_surefire_counts = paper.parse_surefire
jacoco_counter = paper.counter
jacoco_ratio = paper.ratio

# --- misc ----------------------------------------------------------------
utc_now = focal.utc_now
read_json = focal.read_json
write_json = focal.write_json
working_directory = focal.working_directory
Tee = focal.Tee


def preflight_fuse(build_dir: Path, required_major: int) -> dict:
    """Java + Maven + Ollama checks, with the fusion's own model settings."""
    java_path, java_output, java_major = configure_java(required_major)
    mvn_path = find_maven(build_dir)
    check_ollama(config.OLLAMA_SERVER_URL, config.OLLAMA_MODEL)
    return {
        "java": java_output.splitlines()[0],
        "java_major": java_major,
        "required_java_major": required_major,
        "java_path": java_path,
        "maven_path": mvn_path,
        "ollama_server": config.OLLAMA_SERVER_URL,
        "ollama_model": config.OLLAMA_MODEL,
    }


def maven_test_command(maven: str, test_class: str, clean: bool = False) -> list[str]:
    command = [maven, "-q", *SKIP_FLAGS, f"-Dtest={test_class}"]
    if clean:
        command.append("clean")
    command.append("test")
    return command
