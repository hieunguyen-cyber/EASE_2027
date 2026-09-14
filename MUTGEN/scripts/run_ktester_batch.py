#!/usr/bin/env python3
"""Prepare, run, resume, and summarize all KTester focal-method experiments."""

from __future__ import annotations

import argparse
import csv
import json
import os
from pathlib import Path
import re
import shutil
import sys
import time

import run_ktester_focal as focal


DEFAULT_BATCH_STATUS = focal.DEFAULT_RUNS_ROOT / "batch_status.json"


def all_tasks(dataset_path: Path) -> list[tuple[str, dict]]:
    dataset = focal.read_json(dataset_path)
    return [
        (project, task)
        for project, metadata in dataset.items()
        for task in metadata.get("focal-methods", [])
    ]


def selected_tasks(args: argparse.Namespace) -> list[tuple[str, dict]]:
    tasks = all_tasks(Path(args.dataset).resolve())
    projects = set(args.project or [])
    task_ids = set(args.only_task or [])
    if projects:
        tasks = [item for item in tasks if item[0] in projects]
    if task_ids:
        tasks = [item for item in tasks if item[1]["id"] in task_ids]
    if args.limit is not None:
        tasks = tasks[: args.limit]
    return tasks


def prepare_namespace(args: argparse.Namespace, task_id: str) -> argparse.Namespace:
    return argparse.Namespace(
        dataset=args.dataset,
        projects_root=args.projects_root,
        runs_root=args.runs_root,
        task_id=task_id,
    )


def run_namespace(args: argparse.Namespace, task_id: str) -> argparse.Namespace:
    return argparse.Namespace(
        runs_root=args.runs_root,
        task_id=task_id,
        max_feedback_mutants=args.max_feedback_mutants,
        allow_rerun=False,
        skip_ktester_score=args.skip_ktester_score,
    )


def inventory(args: argparse.Namespace) -> int:
    projects_root = Path(args.projects_root).resolve()
    rows = []
    for project, task in selected_tasks(args):
        source_project = projects_root / project
        try:
            module = focal.resolve_module_root(source_project, task)
            references = focal.discover_reference_tests(module, task)
            source_status = "OK"
            module_relative = str(module.relative_to(source_project))
        except Exception as error:
            references = []
            source_status = f"ERROR: {error}"
            module_relative = ""
        rows.append(
            {
                "project": project,
                "task_id": task["id"],
                "module": module_relative,
                "java": focal.required_java_major(project),
                "source": source_status,
                "reference_files": len(references),
            }
        )
    counts = {}
    for row in rows:
        counts[row["project"]] = counts.get(row["project"], 0) + 1
    print(f"Tasks: {len(rows)}")
    print(f"Projects: {counts}")
    print(f"Source errors: {sum(row['source'] != 'OK' for row in rows)}")
    print(f"Tasks without KTester reference: {sum(not row['reference_files'] for row in rows)}")
    print(f"Tasks requiring Java 21: {sum(row['java'] == 21 for row in rows)}")
    if args.verbose:
        for row in rows:
            print(json.dumps(row, ensure_ascii=False))
    return 0 if all(row["source"] == "OK" for row in rows) else 1


def prepare_all(args: argparse.Namespace) -> int:
    tasks = selected_tasks(args)
    runs_root = Path(args.runs_root).resolve()
    prepared = 0
    errors = []
    for index, (project, task) in enumerate(tasks, start=1):
        print(f"\n[prepare {index}/{len(tasks)}] {project}/{task['id']}")
        try:
            focal.prepare(prepare_namespace(args, task["id"]))
            prepared += 1
        except Exception as error:
            errors.append({"project": project, "task_id": task["id"], "error": str(error)})
            print(f"PREPARE_FAILED: {error}", file=sys.stderr)
            if args.stop_on_error:
                break
    manifest = {
        "schema_version": 1,
        "created_at": focal.utc_now(),
        "dataset": str(Path(args.dataset).resolve()),
        "runs_root": str(runs_root),
        "selected": len(tasks),
        "prepared": prepared,
        "errors": errors,
        "task_ids": [task["id"] for _, task in tasks],
    }
    focal.write_json(runs_root / "batch_manifest.json", manifest)
    print(f"\nPrepared {prepared}/{len(tasks)}; errors={len(errors)}")
    print(f"Batch manifest: {runs_root / 'batch_manifest.json'}")
    return 0 if not errors else 1


def validate_all(args: argparse.Namespace) -> int:
    """Offline build/PIT smoke test using one focal method per selected project."""
    representatives = []
    seen = set()
    for project, task in selected_tasks(args):
        if project not in seen:
            seen.add(project)
            representatives.append((project, task))
    failures = 0
    for index, (project, task) in enumerate(representatives, start=1):
        print(f"\n[validate {index}/{len(representatives)}] {project}/{task['id']}")
        try:
            focal.prepare(prepare_namespace(args, task["id"]))
            run_dir = Path(args.runs_root).resolve() / task["id"]
            manifest_path = run_dir / "manifest.json"
            manifest = focal.read_json(manifest_path)
            build_dir = Path(manifest["build_dir"])
            _, java_output, java_major = focal.configure_java(
                int(manifest["required_java_major"])
            )
            maven = focal.find_maven(build_dir)
            logs = run_dir / "logs"
            focal.run_command(
                [maven, "-q", "-Drat.skip=true", "-DskipTests", "compile"],
                build_dir,
                logs / "00_validate_compile.log",
                "validate-compile",
            )
            focal.run_command(
                [
                    maven,
                    "-q",
                    "-Drat.skip=true",
                    f"-Dtest={task['test-class']}",
                    "test",
                ],
                build_dir,
                logs / "00_validate_test.log",
                "validate-test",
            )
            focal.run_command(
                focal.pit_command(task, maven),
                build_dir,
                logs / "00_validate_pit.log",
                "validate-pit",
            )
            report = focal.newest_mutation_report(build_dir)
            source = build_dir / task["source-path"]
            mutants = focal.focal_mutations(report, task, source)
            if not mutants:
                raise RuntimeError("PIT produced no mutants for the representative focal method")
            manifest["validation"] = {
                "status": "PASSED",
                "validated_at": focal.utc_now(),
                "java": java_output.splitlines()[0],
                "java_major": java_major,
                "focal_mutants": len(mutants),
            }
            focal.write_json(manifest_path, manifest)
        except Exception as error:
            failures += 1
            print(f"VALIDATION_FAILED {project}/{task['id']}: {error}", file=sys.stderr)
            manifest_path = Path(args.runs_root).resolve() / task["id"] / "manifest.json"
            if manifest_path.exists():
                manifest = focal.read_json(manifest_path)
                manifest["validation"] = {
                    "status": "FAILED",
                    "validated_at": focal.utc_now(),
                    "error": str(error),
                }
                focal.write_json(manifest_path, manifest)
            if args.stop_on_error:
                break
    print(f"\nValidated {len(representatives) - failures}/{len(representatives)} projects")
    return 0 if failures == 0 else 1


def reset_interrupted_run(manifest_path: Path, reason: str) -> None:
    manifest = focal.read_json(manifest_path)
    history = manifest.setdefault("attempt_history", [])
    history.append(
        {
            "previous_state": manifest.get("state"),
            "error": manifest.get("error"),
            "reset_at": focal.utc_now(),
            "reason": reason,
        }
    )
    build_dir = Path(manifest.get("build_dir", manifest["workspace"]))
    test_path = build_dir / manifest["task"]["test-path"]
    test_path.parent.mkdir(parents=True, exist_ok=True)
    test_path.write_text(focal.make_scaffold(manifest["task"]), encoding="utf-8")
    backup = test_path.with_suffix(test_path.suffix + ".bak")
    if backup.exists():
        backup.unlink()
    manifest["state"] = "prepared"
    manifest.pop("error", None)
    focal.write_json(manifest_path, manifest)


def ratio(numerator, denominator, digits: int = 6):
    if numerator is None or not denominator:
        return None
    return round(numerator / denominator, digits)


def difference(left, right, digits: int = 6):
    if left is None or right is None:
        return None
    return round(left - right, digits)


ROW_FIELDS = [
    # Identity
    "project",
    "task_id",
    "focal_class",
    "focal_method",
    "state",
    # MUTGEN generated test suite
    "mutgen_tests_declared",
    "mutgen_tests_executed",
    "mutgen_tests_passing",
    "mutgen_tests_failing",
    "mutgen_suite_green",
    # Focal mutation pool and the empty-scaffold baseline
    "focal_mutants",
    "baseline_detected",
    "baseline_score",
    # MUTGEN mutation results
    "mutgen_detected",
    "mutgen_survived",
    "mutgen_no_coverage",
    "mutgen_score",
    "mutgen_score_gain",
    "mutgen_killed_per_test",
    # KTester reference
    "ktester_status",
    "ktester_tests",
    "ktester_mutants",
    "ktester_detected",
    "ktester_score",
    "ktester_killed_per_test",
    "ktester_shadow_class",
    # Head-to-head
    "mutgen_minus_ktester",
    "winner",
    # Diagnostics
    "generation_error",
    "error",
]


def redeclares_focal_class(reference_files: list[str], focal_fqn: str | None) -> bool | None:
    simple = (focal_fqn or "").rsplit(".", 1)[-1]
    if not simple or not reference_files:
        return None
    pattern = re.compile(rf"\b(?:class|interface|enum|record)\s+{re.escape(simple)}\b")
    for reference in reference_files:
        path = Path(reference)
        if not path.is_file():
            continue
        if pattern.search(path.read_text(encoding="utf-8", errors="replace")):
            return True
    return False


def verdict(mutgen_score, ktester_score) -> str | None:
    if mutgen_score is None or ktester_score is None:
        return None
    if mutgen_score > ktester_score:
        return "mutgen"
    if mutgen_score < ktester_score:
        return "ktester"
    return "tie"


def free_gib(path: Path) -> float:
    return shutil.disk_usage(path).free / 1024**3


def reclaim_build_output(run_dir: Path) -> float:
    """Drop Maven target trees once results/ holds this task's artifacts.

    Every scored artifact (PIT XML, generated test, summary) is copied into
    results/ by the focal runner, so target/ is reproducible scratch space.
    Set MUTGEN_KEEP_BUILD_OUTPUT=1 to keep it for debugging.
    """
    if os.getenv("MUTGEN_KEEP_BUILD_OUTPUT") == "1":
        return 0.0
    if not (run_dir / "results/summary.json").is_file():
        return 0.0
    freed = 0.0
    for target in run_dir.rglob("target"):
        if not target.is_dir():
            continue
        try:
            freed += sum(
                item.stat().st_size for item in target.rglob("*") if item.is_file()
            ) / 1024**3
        except OSError:
            pass
        shutil.rmtree(target, ignore_errors=True)
    return freed


def collect_rows(args: argparse.Namespace) -> list[dict]:
    runs_root = Path(args.runs_root).resolve()
    rows = []
    for project, task in selected_tasks(args):
        manifest_path = runs_root / task["id"] / "manifest.json"
        row = {field: None for field in ROW_FIELDS}
        row.update(
            {
                "project": project,
                "task_id": task["id"],
                "focal_class": task.get("class"),
                "focal_method": task.get("method-name"),
                "state": "not_prepared",
            }
        )
        if manifest_path.exists():
            manifest = focal.read_json(manifest_path)
            row["state"] = manifest.get("state", "unknown")
            row["error"] = manifest.get("error")
            summary_path = manifest.get("summary")
            if summary_path and Path(summary_path).is_file():
                summary = focal.read_json(Path(summary_path))
                final = summary.get("final") or {}
                initial = summary.get("initial") or {}
                tests = summary.get("mutgen_tests") or {}
                ktester = summary.get("ktester") or {}

                row["generation_error"] = (
                    "yes" if summary.get("generation_error") else "no"
                )
                row["mutgen_tests_declared"] = tests.get("declared_test_methods")
                row["mutgen_tests_executed"] = tests.get("executed_tests")
                row["mutgen_tests_passing"] = tests.get("passing_tests")
                row["mutgen_tests_failing"] = tests.get("failing_tests")
                row["mutgen_suite_green"] = tests.get("suite_green")

                row["focal_mutants"] = final.get("total")
                row["baseline_detected"] = initial.get("detected")
                row["baseline_score"] = initial.get("mutation_score")

                row["mutgen_detected"] = final.get("detected")
                row["mutgen_survived"] = final.get("survived")
                row["mutgen_no_coverage"] = final.get("no_coverage")
                row["mutgen_score"] = final.get("mutation_score")
                row["mutgen_score_gain"] = summary.get("score_delta")
                row["mutgen_killed_per_test"] = ratio(
                    final.get("detected"), tests.get("declared_test_methods"), 3
                )

                row["ktester_status"] = ktester.get("status")
                row["ktester_tests"] = ktester.get("raw_test_count")
                stats = ktester.get("stats") or {}
                row["ktester_mutants"] = stats.get("total")
                row["ktester_detected"] = stats.get("detected")
                row["ktester_score"] = stats.get("mutation_score")
                row["ktester_killed_per_test"] = ratio(
                    stats.get("detected"), ktester.get("raw_test_count"), 3
                )

                recorded_delta = summary.get("mutgen_minus_ktester")
                row["ktester_shadow_class"] = redeclares_focal_class(
                    ktester.get("reference_files")
                    or manifest.get("isolation", {}).get("ktester_tests", []),
                    task.get("class"),
                )
                row["mutgen_minus_ktester"] = (
                    recorded_delta
                    if recorded_delta is not None
                    else difference(row["mutgen_score"], row["ktester_score"])
                )
                row["winner"] = verdict(row["mutgen_score"], row["ktester_score"])
        rows.append(row)
    return rows


def aggregate_rows(rows: list[dict]) -> dict:
    states = {}
    for row in rows:
        states[row["state"]] = states.get(row["state"], 0) + 1
    mutgen_rows = [row for row in rows if row["mutgen_score"] is not None]
    ktester_rows = [row for row in rows if row["ktester_score"] is not None]
    paired = [
        row
        for row in rows
        if row["mutgen_score"] is not None and row["ktester_score"] is not None
    ]

    def mean(values):
        return round(sum(values) / len(values), 6) if values else None

    def micro(rows_to_score, detected_key, total_key):
        total = sum(row[total_key] for row in rows_to_score)
        detected = sum(row[detected_key] for row in rows_to_score)
        return round(detected / total, 6) if total else None

    return {
        "updated_at": focal.utc_now(),
        "tasks": len(rows),
        "states": dict(sorted(states.items())),
        "mutgen_scored_tasks": len(mutgen_rows),
        "ktester_scored_tasks": len(ktester_rows),
        "paired_tasks": len(paired),
        "mutgen_macro_score": mean([row["mutgen_score"] for row in mutgen_rows]),
        "mutgen_micro_score": micro(mutgen_rows, "mutgen_detected", "focal_mutants"),
        "ktester_macro_score": mean([row["ktester_score"] for row in ktester_rows]),
        "ktester_micro_score": micro(
            ktester_rows, "ktester_detected", "ktester_mutants"
        ),
        "paired_mutgen_macro_score": mean([row["mutgen_score"] for row in paired]),
        "paired_ktester_macro_score": mean([row["ktester_score"] for row in paired]),
        "paired_mean_delta": mean([row["mutgen_minus_ktester"] for row in paired]),
        "mutgen_tests_declared_total": sum(
            row["mutgen_tests_declared"] or 0 for row in rows
        ),
        "mutgen_tests_passing_total": sum(
            row["mutgen_tests_passing"] or 0 for row in rows
        ),
        "mutgen_tests_failing_total": sum(
            row["mutgen_tests_failing"] or 0 for row in rows
        ),
        "mutgen_green_suites": sum(row["mutgen_suite_green"] is True for row in rows),
        "mutgen_mean_tests_per_task": mean(
            [
                row["mutgen_tests_declared"]
                for row in rows
                if row["mutgen_tests_declared"] is not None
            ]
        ),
        "mutgen_macro_score_gain": mean(
            [
                row["mutgen_score_gain"]
                for row in rows
                if row["mutgen_score_gain"] is not None
            ]
        ),
        "paired_wins_mutgen": sum(row["winner"] == "mutgen" for row in rows),
        "paired_wins_ktester": sum(row["winner"] == "ktester" for row in rows),
        "paired_ties": sum(row["winner"] == "tie" for row in rows),
        "generation_errors": sum(row["generation_error"] == "yes" for row in rows),
        "ktester_shadow_class_tasks": sum(
            row["ktester_shadow_class"] is True for row in rows
        ),
        "ktester_reference_statuses": {
            status: sum(row["ktester_status"] == status for row in rows)
            for status in sorted(
                {row["ktester_status"] for row in rows if row["ktester_status"]}
            )
        },
    }


def write_status(args: argparse.Namespace) -> dict:
    runs_root = Path(args.runs_root).resolve()
    rows = collect_rows(args)
    aggregate = aggregate_rows(rows)
    focal.write_json(runs_root / "batch_status.json", {"aggregate": aggregate, "tasks": rows})
    csv_path = runs_root / "batch_status.csv"
    csv_path.parent.mkdir(parents=True, exist_ok=True)
    with csv_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=ROW_FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    return aggregate


def status_all(args: argparse.Namespace) -> int:
    aggregate = write_status(args)
    print(json.dumps(aggregate, indent=2, ensure_ascii=False))
    print(f"JSON: {Path(args.runs_root).resolve() / 'batch_status.json'}")
    print(f"CSV:  {Path(args.runs_root).resolve() / 'batch_status.csv'}")
    return 0


def run_all(args: argparse.Namespace) -> int:
    tasks = selected_tasks(args)
    runs_root = Path(args.runs_root).resolve()
    if not args.dry_run:
        server_url = os.getenv(
            "OLLAMA_SERVER_URL", "http://10.148.182.141:11434"
        ).rstrip("/")
        raw_model = os.getenv("OLLAMA_MODEL", "qwen2.5-coder:32b")
        print(f"Checking Ollama {server_url} / {raw_model}...")
        focal.check_ollama(server_url, raw_model)
    attempted = 0
    failures = 0
    for index, (project, task) in enumerate(tasks, start=1):
        if args.max_tasks is not None and attempted >= args.max_tasks:
            break
        if args.dry_run:
            print(f"[queued {index}/{len(tasks)}] {project}/{task['id']}")
            attempted += 1
            continue
        run_dir = runs_root / task["id"]
        manifest_path = run_dir / "manifest.json"
        if not manifest_path.exists():
            if args.no_prepare:
                print(f"[skip {index}/{len(tasks)}] not prepared: {task['id']}")
                continue
            try:
                focal.prepare(prepare_namespace(args, task["id"]))
            except Exception as error:
                failures += 1
                print(f"PREPARE_FAILED {project}/{task['id']}: {error}", file=sys.stderr)
                if args.stop_on_error:
                    break
                continue

        manifest = focal.read_json(manifest_path)
        state = manifest.get("state")
        if state == "completed":
            print(f"[skip {index}/{len(tasks)}] completed: {project}/{task['id']}")
            continue
        if state == "running":
            reset_interrupted_run(manifest_path, "automatic recovery of interrupted run")
        elif state == "failed":
            if not args.retry_failed:
                print(f"[skip {index}/{len(tasks)}] {state}: {project}/{task['id']}")
                continue
            reset_interrupted_run(manifest_path, "batch --retry-failed")

        available = free_gib(runs_root)
        if available < args.min_free_gib:
            print(
                f"ABORT: chỉ còn {available:.1f} GiB trống (< {args.min_free_gib} GiB). "
                "Batch dừng an toàn; giải phóng dung lượng rồi chạy lại để tiếp tục.",
                file=sys.stderr,
            )
            break

        print(
            f"\n[run {index}/{len(tasks)}] {project}/{task['id']} "
            f"(free {available:.1f} GiB)"
        )
        attempted += 1
        try:
            focal.run(run_namespace(args, task["id"]))
        except (Exception, SystemExit) as error:
            failures += 1
            print(f"RUN_FAILED {project}/{task['id']}: {error}", file=sys.stderr)
            if args.stop_on_error:
                write_status(args)
                break
        freed = reclaim_build_output(run_dir)
        if freed > 0.05:
            print(f"[cleanup] {task['id']}: giải phóng {freed:.2f} GiB target/")
        write_status(args)
        if args.delay_seconds > 0:
            time.sleep(args.delay_seconds)

    aggregate = write_status(args)
    print("\nBatch checkpoint:")
    print(json.dumps(aggregate, indent=2, ensure_ascii=False))
    return 0 if failures == 0 else 1


def add_selection_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--dataset", default=str(focal.DEFAULT_DATASET))
    parser.add_argument("--projects-root", default=str(focal.DEFAULT_PROJECTS_ROOT))
    parser.add_argument("--runs-root", default=str(focal.DEFAULT_RUNS_ROOT))
    parser.add_argument("--project", action="append", help="Repeat to select projects.")
    parser.add_argument("--only-task", action="append", help="Repeat to select task IDs.")
    parser.add_argument("--limit", type=int, help="Limit selected dataset tasks.")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Batch runner for KTester/MUTGEN.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    inventory_parser = subparsers.add_parser("inventory")
    add_selection_arguments(inventory_parser)
    inventory_parser.add_argument("--verbose", action="store_true")
    inventory_parser.set_defaults(handler=inventory)

    prepare_parser = subparsers.add_parser("prepare-all")
    add_selection_arguments(prepare_parser)
    prepare_parser.add_argument("--stop-on-error", action="store_true")
    prepare_parser.set_defaults(handler=prepare_all)

    validate_parser = subparsers.add_parser("validate-all")
    add_selection_arguments(validate_parser)
    validate_parser.add_argument("--stop-on-error", action="store_true")
    validate_parser.set_defaults(handler=validate_all)

    run_parser = subparsers.add_parser("run-all")
    add_selection_arguments(run_parser)
    run_parser.add_argument("--max-tasks", type=int, help="Max new attempts this invocation.")
    run_parser.add_argument("--max-feedback-mutants", type=int, default=80)
    run_parser.add_argument("--skip-ktester-score", action="store_true")
    run_parser.add_argument("--retry-failed", action="store_true")
    run_parser.add_argument("--no-prepare", action="store_true")
    run_parser.add_argument("--stop-on-error", action="store_true")
    run_parser.add_argument("--delay-seconds", type=float, default=0.0)
    run_parser.add_argument(
        "--min-free-gib",
        type=float,
        default=float(os.getenv("MUTGEN_MIN_FREE_GIB", "6")),
        help="Stop the batch before free disk space falls below this.",
    )
    run_parser.add_argument("--dry-run", action="store_true")
    run_parser.set_defaults(handler=run_all)

    status_parser = subparsers.add_parser("status-all")
    add_selection_arguments(status_parser)
    status_parser.set_defaults(handler=status_all)
    return parser


def main() -> int:
    args = build_parser().parse_args()
    try:
        return args.handler(args)
    except (RuntimeError, OSError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        print("Interrupted; completed task checkpoints were preserved.", file=sys.stderr)
        return 130


if __name__ == "__main__":
    raise SystemExit(main())
