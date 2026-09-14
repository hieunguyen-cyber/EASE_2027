#!/usr/bin/env python3
"""Render the KTester-vs-MUTGEN result table from a batch run.

Reads ``batch_status.json`` produced by ``run_ktester_batch.py status-all`` and
writes a Markdown report plus a console table.  Every metric is scoped to the
focal method, exactly as PIT was configured during the run.

    python3 scripts/report_ktester_table.py
    python3 scripts/report_ktester_table.py --runs-root ../runs/ktester_qwen25_coder_32b
"""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path


MUTGEN_ROOT = Path(__file__).resolve().parents[1]
REPOSITORY_ROOT = MUTGEN_ROOT.parent
DEFAULT_RUNS_ROOT = Path(
    os.getenv("MUTGEN_RUNS_ROOT")
    or REPOSITORY_ROOT / "runs/ktester_qwen25_coder_32b"
)

# (column header, row key, formatter)
COLUMNS = [
    ("Project", "project", "text"),
    ("Focal method", "task_id", "text"),
    ("State", "state", "text"),
    ("Tests", "mutgen_tests_declared", "int"),
    ("Pass", "mutgen_tests_passing", "int"),
    ("Fail", "mutgen_tests_failing", "int"),
    ("Mutants", "focal_mutants", "int"),
    ("Base MS", "baseline_score", "pct"),
    ("Killed", "mutgen_detected", "int"),
    ("Surv", "mutgen_survived", "int"),
    ("NoCov", "mutgen_no_coverage", "int"),
    ("MutGen MS", "mutgen_score", "pct"),
    ("Gain", "mutgen_score_gain", "signed_pct"),
    ("K/test", "mutgen_killed_per_test", "num"),
    ("KT tests", "ktester_tests", "int"),
    ("KT MS", "ktester_score", "pct"),
    ("Δ MS", "mutgen_minus_ktester", "signed_pct"),
    ("Winner", "winner", "text"),
    ("KT stub?", "ktester_shadow_class", "flag"),
]


def format_cell(value, kind: str) -> str:
    if value is None or value == "":
        return "-"
    if kind == "pct":
        return f"{value * 100:.1f}%"
    if kind == "signed_pct":
        return f"{value * 100:+.1f}pp"
    if kind == "num":
        return f"{value:.2f}"
    if kind == "int":
        return str(value)
    if kind == "flag":
        return "STUB" if value else ""
    if isinstance(value, bool):
        return "yes" if value else "no"
    return str(value)


def markdown_table(rows: list[dict]) -> str:
    header = "| " + " | ".join(name for name, _, _ in COLUMNS) + " |"
    divider = "| " + " | ".join("---" for _ in COLUMNS) + " |"
    lines = [header, divider]
    for row in rows:
        lines.append(
            "| "
            + " | ".join(format_cell(row.get(key), kind) for _, key, kind in COLUMNS)
            + " |"
        )
    return "\n".join(lines)


def console_table(rows: list[dict]) -> str:
    table = [[name for name, _, _ in COLUMNS]]
    for row in rows:
        table.append(
            [format_cell(row.get(key), kind) for _, key, kind in COLUMNS]
        )
    widths = [max(len(line[index]) for line in table) for index in range(len(COLUMNS))]
    lines = []
    for position, line in enumerate(table):
        lines.append(
            "  ".join(cell.ljust(widths[index]) for index, cell in enumerate(line))
        )
        if position == 0:
            lines.append("  ".join("-" * width for width in widths))
    return "\n".join(lines)


AGGREGATE_LABELS = [
    ("tasks", "Focal methods selected"),
    ("mutgen_scored_tasks", "MUTGEN scored"),
    ("ktester_scored_tasks", "KTester scored"),
    ("paired_tasks", "Paired (both scored)"),
    ("generation_errors", "Tasks with a generation error"),
    ("mutgen_tests_declared_total", "MUTGEN test methods generated"),
    ("mutgen_tests_passing_total", "MUTGEN test methods passing"),
    ("mutgen_tests_failing_total", "MUTGEN test methods failing"),
    ("mutgen_green_suites", "Green MUTGEN suites"),
    ("mutgen_mean_tests_per_task", "Mean test methods per focal method"),
    ("mutgen_macro_score", "MUTGEN mutation score (macro)"),
    ("mutgen_micro_score", "MUTGEN mutation score (micro)"),
    ("mutgen_macro_score_gain", "MUTGEN gain over empty scaffold (macro)"),
    ("ktester_macro_score", "KTester mutation score (macro)"),
    ("ktester_micro_score", "KTester mutation score (micro)"),
    ("paired_mutgen_macro_score", "Paired MUTGEN mutation score (macro)"),
    ("paired_ktester_macro_score", "Paired KTester mutation score (macro)"),
    ("paired_mean_delta", "Paired mean delta (MUTGEN - KTester)"),
    ("paired_wins_mutgen", "Focal methods where MUTGEN wins"),
    ("paired_wins_ktester", "Focal methods where KTester wins"),
    ("paired_ties", "Ties"),
    ("ktester_shadow_class_tasks", "KTester tests that redeclare the focal class"),
]


def summary_block(aggregate: dict) -> str:
    lines = ["| Metric | Value |", "| --- | --- |"]
    for key, label in AGGREGATE_LABELS:
        if key not in aggregate:
            continue
        value = aggregate[key]
        if value is None:
            rendered = "-"
        elif isinstance(value, float):
            rendered = f"{value:.4f}"
        else:
            rendered = str(value)
        lines.append(f"| {label} | {rendered} |")
    states = aggregate.get("states") or {}
    if states:
        rendered = ", ".join(f"{name}={count}" for name, count in states.items())
        lines.append(f"| Task states | {rendered} |")
    statuses = aggregate.get("ktester_reference_statuses") or {}
    if statuses:
        rendered = ", ".join(f"{name}={count}" for name, count in statuses.items())
        lines.append(f"| KTester reference statuses | {rendered} |")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runs-root", default=str(DEFAULT_RUNS_ROOT))
    parser.add_argument(
        "--only-completed",
        action="store_true",
        help="Show only focal methods whose run reached the completed state.",
    )
    parser.add_argument(
        "--sort-by",
        default="project",
        help="Row key to sort by, for example mutgen_minus_ktester.",
    )
    parser.add_argument("--output", default=None, help="Markdown output path.")
    args = parser.parse_args()

    runs_root = Path(args.runs_root).resolve()
    status_path = runs_root / "batch_status.json"
    if not status_path.is_file():
        raise SystemExit(
            f"Không tìm thấy {status_path}. Chạy trước:\n"
            f"  python3 scripts/run_ktester_batch.py status-all --runs-root {runs_root}"
        )

    status = json.loads(status_path.read_text(encoding="utf-8"))
    aggregate = status.get("aggregate", {})
    rows = status.get("tasks", [])
    if args.only_completed:
        rows = [row for row in rows if row.get("state") == "completed"]

    def sort_key(row):
        value = row.get(args.sort_by)
        return (value is None, value if value is not None else 0, row.get("task_id") or "")

    rows = sorted(rows, key=sort_key)

    output = Path(args.output) if args.output else runs_root / "results_table.md"
    document = "\n".join(
        [
            "# MUTGEN vs KTester — repository-level focal methods",
            "",
            f"- Runs root: `{runs_root}`",
            f"- Generated: {aggregate.get('updated_at', 'unknown')}",
            "",
            "All mutation metrics are scoped to the focal method only.",
            "`Base MS` is the empty-scaffold baseline, `Gain` is MUTGEN minus that",
            "baseline in percentage points, and `Δ MS` is MUTGEN minus KTester.",
            "",
            "`KT stub?` marks a KTester test that redeclares the focal class inside the",
            "test file and therefore exercises its own stub instead of the repository",
            "class. PIT correctly reports every focal mutant as NO_COVERAGE for those,",
            "so their `KT MS` of 0% is a property of the KTester artifact, not a",
            "measurement error.",
            "",
            "## Aggregate",
            "",
            summary_block(aggregate),
            "",
            "## Per focal method",
            "",
            markdown_table(rows),
            "",
        ]
    )
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(document, encoding="utf-8")

    print(console_table(rows))
    print()
    print(summary_block(aggregate))
    print()
    print(f"Markdown: {output}")
    print(f"CSV:      {runs_root / 'batch_status.csv'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
