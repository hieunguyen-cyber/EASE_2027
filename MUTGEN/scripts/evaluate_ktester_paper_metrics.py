#!/usr/bin/env python3
"""Evaluate saved MUTGEN tests with the automatic metrics from KTester.

This script never invokes an LLM. It rebuilds the final test artifact saved by
the KTester batch runner, runs it under JaCoCo, and scopes coverage to the exact
focal-method overload. Infrastructure failures and generation failures remain
separate so a scaffold left behind after an LLM error cannot inflate CPR/EPR.
"""

from __future__ import annotations

import argparse
import csv
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET


SCRIPT_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPT_DIR))
import run_ktester_focal as focal  # noqa: E402


SCHEMA_VERSION = 1
JACOCO_VERSION = "0.8.12"
PAPER_KTESTER = {
    "cpr": 1.0,
    "epr": 0.7707,
    "line_coverage": 0.6110,
    "branch_coverage": 0.5259,
    "line_coverage_passed": 0.5449,
    "branch_coverage_passed": 0.4623,
    "average_time_seconds": 152.68,
    "average_test_cases": 7.33,
}


ROW_FIELDS = [
    "project",
    "task_id",
    "status",
    "in_protocol_denominator",
    "generation_error",
    "compile_pass",
    "execution_pass",
    "test_cases",
    "tests_executed",
    "tests_failed",
    "line_missed",
    "line_covered",
    "line_coverage",
    "branch_missed",
    "branch_covered",
    "branch_coverage",
    "line_coverage_passed",
    "branch_coverage_passed",
    "generation_seconds",
    "evaluation_seconds",
    "error",
]


def run_process(command: list[str], cwd: Path, log_path: Path, timeout: int) -> subprocess.CompletedProcess[str]:
    started = time.monotonic()
    try:
        result = subprocess.run(
            command,
            cwd=cwd,
            capture_output=True,
            text=True,
            timeout=timeout,
            check=False,
        )
    except subprocess.TimeoutExpired as error:
        stdout = error.stdout.decode(errors="replace") if isinstance(error.stdout, bytes) else (error.stdout or "")
        stderr = error.stderr.decode(errors="replace") if isinstance(error.stderr, bytes) else (error.stderr or "")
        log_path.parent.mkdir(parents=True, exist_ok=True)
        log_path.write_text(
            "$ " + " ".join(command) + f"\n\nTIMEOUT after {timeout}s\n{stdout}\n[stderr]\n{stderr}",
            encoding="utf-8",
            errors="replace",
        )
        raise RuntimeError(f"command timed out after {timeout}s") from error
    elapsed = time.monotonic() - started
    log_path.parent.mkdir(parents=True, exist_ok=True)
    with log_path.open("a", encoding="utf-8", errors="replace") as handle:
        handle.write(
            "$ " + " ".join(command) + f"\n# exit={result.returncode}, elapsed={elapsed:.3f}s\n\n"
            + result.stdout
            + ("\n[stderr]\n" + result.stderr if result.stderr else "")
            + "\n"
        )
    return result


def enable_jacoco_argline(pom_path: Path) -> bytes:
    """Make an explicit Surefire argLine preserve JaCoCo's injected agent."""
    original = pom_path.read_bytes()
    tree = ET.parse(pom_path)
    root = tree.getroot()
    namespace = root.tag.partition("}")[0].removeprefix("{") if root.tag.startswith("{") else ""

    def q(name: str) -> str:
        return f"{{{namespace}}}{name}" if namespace else name

    for plugin in root.findall(f".//{q('plugin')}"):
        artifact = plugin.find(q("artifactId"))
        if artifact is None or artifact.text != "maven-surefire-plugin":
            continue
        configuration = plugin.find(q("configuration"))
        if configuration is None:
            configuration = ET.SubElement(plugin, q("configuration"))
        arg_line = configuration.find(q("argLine"))
        if arg_line is None:
            arg_line = ET.SubElement(configuration, q("argLine"))
        current = (arg_line.text or "").strip()
        if "argline" not in current.lower():
            arg_line.text = ("@{argLine} " + current).strip()
        break
    if namespace:
        ET.register_namespace("", namespace)
        ET.register_namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")
    tree.write(pom_path, encoding="UTF-8", xml_declaration=True)
    return original


def parse_surefire(build_dir: Path, test_class: str) -> tuple[int, int]:
    executed = 0
    failed = 0
    report_root = build_dir / "target/surefire-reports"
    for report in report_root.glob("TEST-*.xml"):
        try:
            suite = ET.parse(report).getroot()
        except ET.ParseError:
            continue
        name = suite.attrib.get("name", "")
        if name != test_class and not name.startswith(test_class + "$"):
            continue
        executed += int(suite.attrib.get("tests", "0"))
        failed += int(suite.attrib.get("failures", "0"))
        failed += int(suite.attrib.get("errors", "0"))
    return executed, failed


def counter(method: ET.Element, kind: str) -> tuple[int, int]:
    node = method.find(f"counter[@type='{kind}']")
    if node is None:
        return 0, 0
    return int(node.attrib.get("missed", "0")), int(node.attrib.get("covered", "0"))


def ratio(covered: int, missed: int) -> float | None:
    total = covered + missed
    return round(covered / total, 6) if total else None


def recover_generation_seconds(run_dir: Path) -> float | None:
    """Recover MUTGEN wall time, using log lifetime only for crashed runs."""
    log_path = run_dir / "logs/04_mutgen.log"
    if not log_path.is_file():
        return None
    content = log_path.read_text(encoding="utf-8", errors="replace")
    matches = re.findall(r"unit_test_gen\.py, total time: ([0-9.]+) seconds", content)
    if matches:
        return round(float(matches[-1]), 3)

    stat_result = subprocess.run(
        ["stat", "-c", "%W", str(log_path)],
        capture_output=True,
        text=True,
        check=False,
    )
    value = stat_result.stdout.strip()
    try:
        created = float(value) if value and value != "0" else None
    except ValueError:
        created = None
    return round(max(0.0, log_path.stat().st_mtime - created), 3) if created else None


def generated_test_cases(path: Path) -> int:
    """Count generated JUnit methods, excluding the harness-only scaffold."""
    total = focal.count_test_methods(path)
    content = path.read_text(encoding="utf-8", errors="replace") if path.is_file() else ""
    scaffold = 1 if re.search(r"\bvoid\s+scaffoldLoads\s*\(", content) else 0
    return max(0, total - scaffold)


def focal_coverage(report_path: Path, task: dict, source_path: Path) -> dict:
    root = ET.parse(report_path).getroot()
    xml_class_name = task["class"].replace(".", "/")
    expected_method = focal.method_name(task)
    start_line, end_line = focal.focal_line_range(source_path, task)
    matches = []
    for class_node in root.findall(".//class"):
        if class_node.attrib.get("name") != xml_class_name:
            continue
        for method in class_node.findall("method"):
            if method.attrib.get("name") != expected_method:
                continue
            line = int(method.attrib.get("line", "0"))
            if start_line <= line <= end_line:
                matches.append(method)
    if len(matches) != 1:
        found = [
            (method.attrib.get("name"), method.attrib.get("desc"), method.attrib.get("line"))
            for class_node in root.findall(".//class")
            if class_node.attrib.get("name") == xml_class_name
            for method in class_node.findall("method")
            if method.attrib.get("name") == expected_method
        ]
        raise RuntimeError(
            f"expected one JaCoCo method for {task['class']}.{task['method-name']} "
            f"at lines {start_line}-{end_line}; found {found}"
        )
    line_missed, line_covered = counter(matches[0], "LINE")
    branch_missed, branch_covered = counter(matches[0], "BRANCH")
    return {
        "line_missed": line_missed,
        "line_covered": line_covered,
        "line_coverage": ratio(line_covered, line_missed),
        "branch_missed": branch_missed,
        "branch_covered": branch_covered,
        "branch_coverage": ratio(branch_covered, branch_missed),
    }


def blank_row(manifest: dict) -> dict:
    return {
        field: None for field in ROW_FIELDS
    } | {
        "project": manifest.get("project"),
        "task_id": manifest.get("task_id"),
        "status": "unknown",
        "in_protocol_denominator": False,
        "generation_error": False,
    }


def evaluate_task(run_dir: Path, timeout: int, keep_build_output: bool) -> dict:
    manifest_path = run_dir / "manifest.json"
    if not manifest_path.is_file():
        return blank_row({"task_id": run_dir.name}) | {
            "status": "infrastructure_failed",
            "error": "manifest missing",
        }
    manifest = focal.read_json(manifest_path)
    row = blank_row(manifest)
    summary_path = run_dir / "results/summary.json"
    if manifest.get("state") != "completed" or not summary_path.is_file():
        return row | {
            "status": "infrastructure_failed",
            "error": manifest.get("error") or "task did not complete before test evaluation",
        }

    row["in_protocol_denominator"] = True
    summary = focal.read_json(summary_path)
    row["generation_seconds"] = recover_generation_seconds(run_dir)
    generated_test = run_dir / "results/mutgen_test.java"
    if summary.get("generation_error"):
        return row | {
            "status": "generation_failed",
            "generation_error": True,
            "compile_pass": False,
            "execution_pass": False,
            "test_cases": 0,
            "tests_executed": 0,
            "tests_failed": 0,
            "line_missed": 0,
            "line_covered": 0,
            "line_coverage": 0.0,
            "branch_missed": 0,
            "branch_covered": 0,
            "branch_coverage": 0.0,
            "line_coverage_passed": 0.0,
            "branch_coverage_passed": 0.0,
            "error": "MUTGEN generation failed; leftover scaffold excluded",
        }
    if not generated_test.is_file():
        return row | {
            "status": "generation_failed",
            "generation_error": True,
            "compile_pass": False,
            "execution_pass": False,
            "test_cases": 0,
            "tests_executed": 0,
            "tests_failed": 0,
            "line_coverage": 0.0,
            "branch_coverage": 0.0,
            "line_coverage_passed": 0.0,
            "branch_coverage_passed": 0.0,
            "error": "saved MUTGEN test missing",
        }

    build_dir = Path(manifest["build_dir"])
    task = manifest["task"]
    active_test = build_dir / task["test-path"]
    active_test.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(generated_test, active_test)
    row["test_cases"] = generated_test_cases(generated_test)
    log_path = run_dir / "logs/09_paper_metrics.log"
    log_path.unlink(missing_ok=True)
    started = time.monotonic()
    pom_path = build_dir / "pom.xml"
    original_pom = enable_jacoco_argline(pom_path)
    try:
        focal.configure_java(int(manifest.get("required_java_major", 11)))
        maven = focal.find_maven(build_dir)
        common = [maven, "-o", "-q", "-Drat.skip=true", f"-Dtest={task['test-class']}"]
        compile_result = run_process(
            common + ["clean", "test-compile"], build_dir, log_path, timeout
        )
        row["compile_pass"] = compile_result.returncode == 0
        if not row["compile_pass"]:
            row.update(
                status="compile_failed",
                execution_pass=False,
                tests_executed=0,
                tests_failed=0,
                line_coverage=0.0,
                branch_coverage=0.0,
                line_coverage_passed=0.0,
                branch_coverage_passed=0.0,
                error="generated test did not compile; see logs/09_paper_metrics.log",
            )
            return row

        test_result = run_process(
            common + ["-Dmaven.test.failure.ignore=true", "test"],
            build_dir,
            log_path,
            timeout,
        )
        jacoco_exec = build_dir / "target/jacoco.exec"
        if jacoco_exec.is_file() and jacoco_exec.stat().st_size:
            report_result = run_process(
                common + [f"org.jacoco:jacoco-maven-plugin:{JACOCO_VERSION}:report"],
                build_dir,
                log_path,
                timeout,
            )
        else:
            report_result = run_process(
                common
                + [
                    "-Dmaven.test.failure.ignore=true",
                    f"org.jacoco:jacoco-maven-plugin:{JACOCO_VERSION}:prepare-agent",
                    "test",
                    f"org.jacoco:jacoco-maven-plugin:{JACOCO_VERSION}:report",
                ],
                build_dir,
                log_path,
                timeout,
            )
        executed, failed = parse_surefire(build_dir, task["test-class"])
        row["tests_executed"] = executed
        row["tests_failed"] = failed
        row["execution_pass"] = (
            test_result.returncode == 0
            and row["test_cases"] > 0
            and executed > 0
            and failed == 0
        )
        if report_result.returncode != 0:
            raise RuntimeError("JaCoCo report command failed")
        report = build_dir / "target/site/jacoco/jacoco.xml"
        if not report.is_file():
            raise RuntimeError("JaCoCo XML report missing")
        coverage = focal_coverage(report, task, build_dir / task["source-path"])
        row.update(coverage)
        row["line_coverage_passed"] = coverage["line_coverage"] if row["execution_pass"] else 0.0
        row["branch_coverage_passed"] = coverage["branch_coverage"] if row["execution_pass"] else 0.0
        shutil.copy2(report, run_dir / "results/paper_metrics_jacoco.xml")
        if row["test_cases"] == 0:
            row["status"] = "empty_generation"
        else:
            row["status"] = "evaluated" if row["execution_pass"] else "execution_failed"
        if not row["execution_pass"]:
            row["error"] = "generated test did not execute cleanly; see logs/09_paper_metrics.log"
        return row
    except Exception as error:
        row["status"] = "evaluation_failed"
        row["execution_pass"] = False if row.get("compile_pass") else row.get("execution_pass")
        row["error"] = str(error)
        return row
    finally:
        pom_path.write_bytes(original_pom)
        row["evaluation_seconds"] = round(time.monotonic() - started, 3)
        focal.write_json(run_dir / "results/paper_metrics.json", row)
        if not keep_build_output:
            shutil.rmtree(build_dir / "target", ignore_errors=True)


def mean(values: list[float | int | None]) -> float | None:
    present = [float(value) for value in values if value is not None]
    return round(sum(present) / len(present), 6) if present else None


def aggregate(rows: list[dict]) -> dict:
    protocol = [row for row in rows if row["in_protocol_denominator"]]
    denominator = len(protocol)
    compile_passes = sum(row["compile_pass"] is True for row in protocol)
    execution_passes = sum(row["execution_pass"] is True for row in protocol)
    coverage_rows = [row for row in protocol if row["line_coverage"] is not None]
    branch_rows = [row for row in protocol if row["branch_coverage"] is not None]
    test_cases = sum(row["test_cases"] or 0 for row in protocol)
    measured = {
        "cpr": round(compile_passes / denominator, 6) if denominator else None,
        "epr": round(execution_passes / denominator, 6) if denominator else None,
        "line_coverage": mean([row["line_coverage"] for row in coverage_rows]),
        "branch_coverage": mean([row["branch_coverage"] for row in branch_rows]),
        "line_coverage_passed": mean([row["line_coverage_passed"] for row in coverage_rows]),
        "branch_coverage_passed": mean([row["branch_coverage_passed"] for row in branch_rows]),
        "average_test_cases": round(test_cases / denominator, 6) if denominator else None,
        "average_time_seconds": mean([row.get("generation_seconds") for row in protocol]),
    }
    return {
        "schema_version": SCHEMA_VERSION,
        "generated_at": focal.utc_now(),
        "selected_tasks": len(rows),
        "protocol_tasks": denominator,
        "infrastructure_failed_tasks": sum(row["status"] == "infrastructure_failed" for row in rows),
        "generation_failed_tasks": sum(row["status"] == "generation_failed" for row in protocol),
        "empty_generation_tasks": sum(row["status"] == "empty_generation" for row in protocol),
        "evaluation_failed_tasks": sum(row["status"] == "evaluation_failed" for row in protocol),
        "compile_passed_tasks": compile_passes,
        "execution_passed_tasks": execution_passes,
        "test_cases": test_cases,
        "mutgen_qwen": measured,
        "paper_ktester": PAPER_KTESTER,
        "delta_mutgen_minus_paper": {
            key: round(measured[key] - PAPER_KTESTER[key], 6)
            if measured.get(key) is not None else None
            for key in PAPER_KTESTER
        },
    }


def write_outputs(runs_root: Path, rows: list[dict]) -> None:
    summary = aggregate(rows)
    focal.write_json(runs_root / "paper_metrics.json", {"aggregate": summary, "tasks": rows})
    with (runs_root / "paper_metrics.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=ROW_FIELDS)
        writer.writeheader()
        writer.writerows(rows)

    measured = summary["mutgen_qwen"]
    paper = summary["paper_ktester"]
    delta = summary["delta_mutgen_minus_paper"]
    labels = [
        ("CPR", "cpr", "%"),
        ("EPR", "epr", "%"),
        ("LC", "line_coverage", "%"),
        ("BC", "branch_coverage", "%"),
        ("LCP", "line_coverage_passed", "%"),
        ("BCP", "branch_coverage_passed", "%"),
        ("AvT (s)", "average_time_seconds", "number"),
        ("AvTC", "average_test_cases", "number"),
    ]

    def render(value: float | None, kind: str) -> str:
        if value is None:
            return "N/A"
        return f"{value * 100:.2f}%" if kind == "%" else f"{value:.2f}"

    def render_delta(value: float | None, kind: str) -> str:
        if value is None:
            return "N/A"
        return f"{value * 100:+.2f} pp" if kind == "%" else f"{value:+.2f}"

    lines = [
        "# MUTGEN Qwen vs KTester paper metrics",
        "",
        f"Protocol denominator: {summary['protocol_tasks']} locally evaluable tasks; "
        f"{summary['infrastructure_failed_tasks']} infrastructure failures are reported separately.",
        "Generation failures are included in the denominator with zero coverage; leftover scaffolds are excluded.",
        "Harness-only scaffoldLoads methods are excluded from AvTC and cannot make a suite execution-passing.",
        "KTester paper values are from Table 2 of the ICSE 2026 paper (110 tasks, GPT-4o-mini, mean of five runs).",
        "AvT uses MUTGEN's own total-time log; for generation crashes it is reconstructed from the log lifetime.",
        "",
        "| Metric | MUTGEN + Qwen | KTester paper | Delta |",
        "| --- | ---: | ---: | ---: |",
    ]
    for label, key, kind in labels:
        lines.append(
            f"| {label} | {render(measured[key], kind)} | {render(paper[key], kind)} | {render_delta(delta[key], kind)} |"
        )
    lines.extend(
        [
            "",
            "| Diagnostic | Value |",
            "| --- | ---: |",
            f"| Selected tasks | {summary['selected_tasks']} |",
            f"| Protocol tasks | {summary['protocol_tasks']} |",
            f"| Infrastructure failures | {summary['infrastructure_failed_tasks']} |",
            f"| Generation failures | {summary['generation_failed_tasks']} |",
            f"| Empty generations (scaffold only) | {summary['empty_generation_tasks']} |",
            f"| Evaluation failures | {summary['evaluation_failed_tasks']} |",
            f"| Compile-passing classes | {summary['compile_passed_tasks']} |",
            f"| Execution-passing classes | {summary['execution_passed_tasks']} |",
            f"| Counted generated test cases | {summary['test_cases']} |",
            "",
        ]
    )
    (runs_root / "paper_metrics.md").write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runs-root", default=str(focal.DEFAULT_RUNS_ROOT))
    parser.add_argument("--only-task", action="append", default=[])
    parser.add_argument("--timeout", type=int, default=1800)
    parser.add_argument("--keep-build-output", action="store_true")
    parser.add_argument("--no-resume", action="store_true")
    args = parser.parse_args()

    runs_root = Path(args.runs_root).resolve()
    batch_manifest = focal.read_json(runs_root / "batch_manifest.json")
    selected = set(args.only_task)
    task_ids = [task_id for task_id in batch_manifest["task_ids"] if not selected or task_id in selected]
    rows = []
    for index, task_id in enumerate(task_ids, start=1):
        run_dir = runs_root / task_id
        result_path = run_dir / "results/paper_metrics.json"
        saved = focal.read_json(result_path) if result_path.is_file() else None
        resumable = saved and saved.get("status") != "evaluation_failed"
        if not args.no_resume and resumable:
            row = saved
            if row.get("in_protocol_denominator"):
                if row.get("generation_seconds") is None:
                    row["generation_seconds"] = recover_generation_seconds(run_dir)
                test_path = run_dir / "results/mutgen_test.java"
                if not row.get("generation_error") and test_path.is_file():
                    row["test_cases"] = generated_test_cases(test_path)
                    if row["test_cases"] == 0 and row.get("status") == "evaluated":
                        row["status"] = "empty_generation"
                        row["execution_pass"] = False
                focal.write_json(result_path, row)
            print(f"[{index}/{len(task_ids)}] {task_id}: resume {row['status']}")
        else:
            print(f"[{index}/{len(task_ids)}] {task_id}: evaluate", flush=True)
            row = evaluate_task(run_dir, args.timeout, args.keep_build_output)
            if not result_path.is_file():
                result_path.parent.mkdir(parents=True, exist_ok=True)
                focal.write_json(result_path, row)
            print(f"    -> {row['status']}", flush=True)
        rows.append(row)
        write_outputs(runs_root, rows)

    write_outputs(runs_root, rows)
    summary = aggregate(rows)
    print(json.dumps(summary, indent=2, ensure_ascii=False))
    print(f"Markdown: {runs_root / 'paper_metrics.md'}")
    return 0 if summary["evaluation_failed_tasks"] == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
