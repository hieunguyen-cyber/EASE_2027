#!/usr/bin/env python3
"""Drive the fusion over many focal methods, sequentially and resumably.

Sequential by design: one hosted Ollama model serves every generation call, so
parallel tasks would only queue against each other while multiplying the Maven
and PIT load on this machine.

    inventory    list the tasks the dataset offers
    prepare-all  build every isolated workspace (offline)
    run-all      run the fusion, skipping what already completed
    status-all   refresh the aggregate JSON/CSV
"""

from __future__ import annotations

import argparse
import csv
import shutil
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import config
from pipeline import mutgen_bridge as mb

HERE = Path(__file__).resolve().parent


def load_tasks(project: str | None = None) -> list[tuple[str, dict]]:
    dataset = mb.read_json(config.DATASET)
    tasks = []
    for project_key, payload in dataset.items():
        name = payload.get("project-name", project_key)
        if project and name != project:
            continue
        for task in payload.get("focal-methods", []):
            tasks.append((name, task))
    return tasks


def task_state(task_id: str) -> str:
    manifest = config.TASKS_ROOT / task_id / "manifest.json"
    if not manifest.exists():
        return "absent"
    return mb.read_json(manifest).get("state", "unknown")


def task_summary(task_id: str) -> dict | None:
    path = config.TASKS_ROOT / task_id / "results/summary.json"
    return mb.read_json(path) if path.exists() else None


def free_gib(path: Path) -> float:
    usage = shutil.disk_usage(path)
    return usage.free / (1024 ** 3)


def call_focal(command: str, task_id: str, extra: list[str] | None = None) -> int:
    args = [sys.executable, str(HERE / "run_focal.py"), command, "--task-id", task_id]
    args.extend(extra or [])
    return subprocess.run(args, cwd=HERE).returncode


# =========================================================================
def inventory(args: argparse.Namespace) -> int:
    tasks = load_tasks(args.project)
    by_project: dict[str, int] = {}
    for project, _ in tasks:
        by_project[project] = by_project.get(project, 0) + 1
    print(f"{len(tasks)} focal method trong {len(by_project)} project")
    for project, count in sorted(by_project.items()):
        print(f"  {project:32s} {count:3d}")
    states: dict[str, int] = {}
    for _, task in tasks:
        state = task_state(task["id"])
        states[state] = states.get(state, 0) + 1
    print("trạng thái:", ", ".join(f"{k}={v}" for k, v in sorted(states.items())))
    return 0


def prepare_all(args: argparse.Namespace) -> int:
    tasks = load_tasks(args.project)
    if args.max_tasks:
        tasks = tasks[: args.max_tasks]
    failures = []
    for index, (project, task) in enumerate(tasks, start=1):
        task_id = task["id"]
        if task_state(task_id) != "absent" and not args.force:
            continue
        print(f"[{index}/{len(tasks)}] prepare {task_id} ({project})")
        extra = ["--force"] if args.force else []
        if call_focal("prepare", task_id, extra) != 0:
            failures.append(task_id)
    print(f"prepare-all xong. Lỗi: {len(failures)}")
    for task_id in failures:
        print(f"  {task_id}")
    return 0


def run_all(args: argparse.Namespace) -> int:
    tasks = load_tasks(args.project)
    selected = []
    for project, task in tasks:
        state = task_state(task["id"])
        if state == "completed":
            continue
        if state == "failed" and not args.retry_failed:
            continue
        selected.append((project, task))
    if args.max_tasks:
        selected = selected[: args.max_tasks]

    print(f"Sẽ chạy {len(selected)} task. Run root: {config.RUN_ROOT}")
    config.RUN_ROOT.mkdir(parents=True, exist_ok=True)
    for index, (project, task) in enumerate(selected, start=1):
        task_id = task["id"]
        available = free_gib(config.RUN_ROOT)
        if available < args.min_free_gib:
            print(
                f"Dừng: còn {available:.1f} GiB, dưới ngưỡng {args.min_free_gib} GiB. "
                "Dọn bớt rồi chạy lại -- các task đã xong sẽ được bỏ qua."
            )
            break
        state = task_state(task_id)
        if state == "absent":
            print(f"[{index}/{len(selected)}] prepare {task_id}")
            if call_focal("prepare", task_id) != 0:
                continue
        elif state in {"running", "failed"}:
            # An interrupted task left a mutated workspace; rebuild it clean.
            print(f"[{index}/{len(selected)}] reset {task_id} (state={state})")
            if call_focal("prepare", task_id, ["--force"]) != 0:
                continue
        print(f"[{index}/{len(selected)}] run {task_id} ({project})")
        call_focal("run", task_id)
        status_all(args)
    return 0


# Short column name -> key in the arm's summary block.
COVERAGE_COLUMNS = {
    "lc": "line_coverage",
    "bc": "branch_coverage",
    "ic": "instruction_coverage",
    "lcp": "line_coverage_passed",
    "bcp": "branch_coverage_passed",
    "icp": "instruction_coverage_passed",
}


def pct(value) -> float | str:
    return round(value * 100, 2) if isinstance(value, (int, float)) else ""


def arm_columns(prefix: str, arm: dict) -> dict:
    stats = arm.get("stats") or {}
    row = {
        f"{prefix}_tests": arm.get("test_cases", ""),
        f"{prefix}_killed": stats.get("detected", ""),
        f"{prefix}_mutants": stats.get("total", ""),
        f"{prefix}_survived": stats.get("survived", ""),
        f"{prefix}_nocov": stats.get("no_coverage", ""),
        f"{prefix}_ms": pct(stats.get("mutation_score")),
        f"{prefix}_cpr": arm.get("compile_pass", ""),
        f"{prefix}_epr": arm.get("execution_pass", ""),
        f"{prefix}_kpt": arm.get("kills_per_test", ""),
        f"{prefix}_secs": arm.get("generation_seconds", ""),
    }
    for short, key in COVERAGE_COLUMNS.items():
        row[f"{prefix}_{short}"] = pct(arm.get(key))
    return row


def collect_rows() -> list[dict]:
    rows = []
    for project, task in load_tasks():
        task_id = task["id"]
        state = task_state(task_id)
        if state == "absent":
            continue
        summary = task_summary(task_id)
        row = {
            "task_id": task_id,
            "project": project,
            "state": state,
            "status": (summary or {}).get("status", ""),
        }
        if summary and summary.get("status") == "COMPLETED":
            arm_k, arm_f, delta = summary["ktester"], summary["fuse"], summary["delta"]
            row.update(arm_columns("kt", arm_k))
            row.update(arm_columns("f", arm_f))
            row.update(
                {
                    "mutants": (arm_k.get("stats") or {}).get("total", ""),
                    "d_ms_pp": delta.get("mutation_score_points", ""),
                    "d_lc_pp": pct(delta.get("line_coverage")),
                    "d_bc_pp": pct(delta.get("branch_coverage")),
                    "d_ic_pp": pct(delta.get("instruction_coverage")),
                    "d_kpt": delta.get("kills_per_test", ""),
                    "newly_killed": delta.get("newly_killed", ""),
                    "lost_kills": delta.get("lost_kills", ""),
                    "added_tests": delta.get("added_test_methods", ""),
                    "seed_health": summary.get("seed_health", {}).get("status", ""),
                    "suite_green": arm_f.get("execution_pass_new_tests_only", ""),
                }
            )
        rows.append(row)
    # A stable, complete header even when the first task failed early.
    template = {key: "" for row in rows for key in row}
    return [template | row for row in rows]


def _mean(values: list) -> float | None:
    present = [float(v) for v in values if isinstance(v, (int, float))]
    return round(sum(present) / len(present), 4) if present else None


def _rate(flags: list) -> float | None:
    present = [bool(v) for v in flags if isinstance(v, bool)]
    return round(sum(present) / len(present), 4) if present else None


def arm_aggregate(rows: list[dict], prefix: str) -> dict:
    """The eight KTester paper metrics plus the MutGen mutation metrics."""
    return {
        "cpr": _rate([r.get(f"{prefix}_cpr") for r in rows]),
        "epr": _rate([r.get(f"{prefix}_epr") for r in rows]),
        "line_coverage": _mean([r.get(f"{prefix}_lc") for r in rows]),
        "branch_coverage": _mean([r.get(f"{prefix}_bc") for r in rows]),
        "instruction_coverage": _mean([r.get(f"{prefix}_ic") for r in rows]),
        "line_coverage_passed": _mean([r.get(f"{prefix}_lcp") for r in rows]),
        "branch_coverage_passed": _mean([r.get(f"{prefix}_bcp") for r in rows]),
        "instruction_coverage_passed": _mean([r.get(f"{prefix}_icp") for r in rows]),
        "average_test_cases": _mean([r.get(f"{prefix}_tests") for r in rows]),
        "average_time_seconds": _mean([r.get(f"{prefix}_secs") for r in rows]),
        "mutation_score": _mean([r.get(f"{prefix}_ms") for r in rows]),
        "kills_per_test": _mean([r.get(f"{prefix}_kpt") for r in rows]),
        "total_mutants": sum(r.get(f"{prefix}_mutants") or 0 for r in rows),
        "total_killed": sum(r.get(f"{prefix}_killed") or 0 for r in rows),
        "total_survived": sum(r.get(f"{prefix}_survived") or 0 for r in rows),
        "total_no_coverage": sum(r.get(f"{prefix}_nocov") or 0 for r in rows),
    }


def mutator_aggregate(task_ids: list[str]) -> dict:
    """Per-operator kill rates for both arms -- the MutGen paper's RQ2 view."""
    buckets: dict[str, dict] = {}
    for task_id in task_ids:
        summary = task_summary(task_id)
        if not summary or summary.get("status") != "COMPLETED":
            continue
        for arm in ("ktester", "fuse"):
            for mutator, data in (summary[arm].get("by_mutator") or {}).items():
                bucket = buckets.setdefault(
                    mutator,
                    {"kt_total": 0, "kt_killed": 0, "f_total": 0, "f_killed": 0},
                )
                key = "kt" if arm == "ktester" else "f"
                bucket[f"{key}_total"] += data["total"]
                bucket[f"{key}_killed"] += data["detected"]
    for bucket in buckets.values():
        bucket["kt_ms"] = (
            round(bucket["kt_killed"] / bucket["kt_total"] * 100, 2)
            if bucket["kt_total"] else None
        )
        bucket["f_ms"] = (
            round(bucket["f_killed"] / bucket["f_total"] * 100, 2)
            if bucket["f_total"] else None
        )
        bucket["delta_pp"] = (
            round(bucket["f_ms"] - bucket["kt_ms"], 2)
            if bucket["kt_ms"] is not None and bucket["f_ms"] is not None else None
        )
    return dict(sorted(buckets.items(), key=lambda item: -(item[1]["kt_total"])))


def aggregate(rows: list[dict]) -> dict:
    done = [r for r in rows if r["status"] == "COMPLETED"]
    if not done:
        return {"completed": 0}

    from pipeline.metrics import wilcoxon

    deltas = [r["d_ms_pp"] for r in done if isinstance(r["d_ms_pp"], (int, float))]
    wins = sum(1 for d in deltas if d > 0)
    losses = sum(1 for d in deltas if d < 0)
    ktester = arm_aggregate(done, "kt")
    fuse = arm_aggregate(done, "f")

    def paired(metric: str) -> dict:
        return wilcoxon(
            [r.get(f"kt_{metric}") for r in done], [r.get(f"f_{metric}") for r in done]
        )

    return {
        "completed": len(done),
        "selected": len(rows),
        "ktester_arm": ktester,
        "fuse_arm": fuse,
        "paper_ktester_published": mb.PAPER_KTESTER,
        "delta_fuse_minus_ktester": {
            key: round(fuse[key] - ktester[key], 4)
            for key in ktester
            if isinstance(ktester.get(key), (int, float))
            and isinstance(fuse.get(key), (int, float))
        },
        "significance": {
            "mutation_score": paired("ms"),
            "line_coverage": paired("lc"),
            "branch_coverage": paired("bc"),
        },
        "per_method_outcome": {
            "fuse_wins": wins,
            "ktester_wins": losses,
            "ties": len(done) - wins - losses,
        },
        "mutants": {
            "total": ktester["total_mutants"],
            "killed_ktester": ktester["total_killed"],
            "killed_fuse": fuse["total_killed"],
            "newly_killed": sum(r.get("newly_killed") or 0 for r in done),
            "lost_kills": sum(r.get("lost_kills") or 0 for r in done),
        },
        "by_mutator": mutator_aggregate([r["task_id"] for r in done]),
        "green_suites": sum(1 for r in done if r.get("suite_green") is True),
    }


def status_all(args: argparse.Namespace) -> int:
    rows = collect_rows()
    summary = aggregate(rows)
    config.RUN_ROOT.mkdir(parents=True, exist_ok=True)
    mb.write_json(config.RUN_ROOT / "batch_status.json", {"rows": rows, "aggregate": summary})
    if rows:
        with (config.RUN_ROOT / "batch_status.csv").open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()))
            writer.writeheader()
            writer.writerows(rows)
    if getattr(args, "verbose", False):
        print(f"{len(rows)} task, aggregate: {summary}")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    p_inventory = sub.add_parser("inventory")
    p_inventory.add_argument("--project")
    p_inventory.set_defaults(handler=inventory)

    p_prepare = sub.add_parser("prepare-all")
    p_prepare.add_argument("--project")
    p_prepare.add_argument("--max-tasks", type=int)
    p_prepare.add_argument("--force", action="store_true")
    p_prepare.set_defaults(handler=prepare_all)

    p_run = sub.add_parser("run-all")
    p_run.add_argument("--project")
    p_run.add_argument("--max-tasks", type=int)
    p_run.add_argument("--retry-failed", action="store_true")
    p_run.add_argument("--min-free-gib", type=float, default=6.0)
    p_run.set_defaults(handler=run_all)

    p_status = sub.add_parser("status-all")
    p_status.add_argument("--verbose", action="store_true", default=True)
    p_status.set_defaults(handler=status_all)
    return parser


def main() -> int:
    args = build_parser().parse_args()
    return args.handler(args)


if __name__ == "__main__":
    raise SystemExit(main())
