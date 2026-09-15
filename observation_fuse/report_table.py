#!/usr/bin/env python3
"""Render every metric both papers report, for both arms, paired.

Three tables:

1. **Paper metrics** -- the eight KTester metrics (CPR, EPR, LC, BC, LCP, BCP,
   AvTC, AvT) plus the MutGen ones (MS, kills/test), for the KTester arm and
   the fused arm, next to the numbers KTester published.
2. **Per mutation operator** -- the MutGen paper's RQ2 view, computed from PIT's
   own operator names rather than by asking a model to classify them.
3. **Per focal method** -- one row each, for spotting where the fusion helps.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import config
from pipeline import mutgen_bridge as mb
from run_batch import aggregate, collect_rows

# label, aggregate key, published-paper key, unit
PAPER_METRICS = [
    ("CPR", "cpr", "cpr", "rate"),
    ("EPR", "epr", "epr", "rate"),
    ("LC", "line_coverage", "line_coverage", "pct"),
    ("BC", "branch_coverage", "branch_coverage", "pct"),
    ("IC", "instruction_coverage", None, "pct"),
    ("LCP", "line_coverage_passed", "line_coverage_passed", "pct"),
    ("BCP", "branch_coverage_passed", "branch_coverage_passed", "pct"),
    ("ICP", "instruction_coverage_passed", None, "pct"),
    ("AvTC", "average_test_cases", "average_test_cases", "num"),
    ("AvT (s)", "average_time_seconds", "average_time_seconds", "num"),
    ("MS", "mutation_score", None, "pct"),
    ("Kills/test", "kills_per_test", None, "num"),
]

TASK_COLUMNS = [
    ("task_id", "Focal method"),
    ("project", "Project"),
    ("mutants", "Mut"),
    ("kt_tests", "KT n"),
    ("kt_ms", "KT MS%"),
    ("kt_lc", "KT LC%"),
    ("kt_bc", "KT BC%"),
    ("f_tests", "F n"),
    ("f_ms", "F MS%"),
    ("f_lc", "F LC%"),
    ("f_bc", "F BC%"),
    ("d_ms_pp", "ΔMS"),
    ("d_lc_pp", "ΔLC"),
    ("d_bc_pp", "ΔBC"),
    ("newly_killed", "+kill"),
    ("lost_kills", "-kill"),
    ("winner", "Winner"),
]


def fmt(value, unit: str, already_percent: bool = False) -> str:
    if value is None or value == "":
        return "—"
    if unit == "rate":
        return f"{value * 100:.2f}%"
    if unit == "pct":
        return f"{value:.2f}%" if already_percent else f"{value * 100:.2f}%"
    return f"{value:.2f}"


def scratch_score(task_id: str) -> float | str:
    """MutGen-from-scratch mutation score from the earlier comparison run."""
    path = config.MUTGEN_SCRATCH_RUN / task_id / "results/summary.json"
    if not path.exists():
        return ""
    score = ((mb.read_json(path) or {}).get("final") or {}).get("mutation_score")
    return round(score * 100, 2) if score is not None else ""


def markdown(headers: list[str], rows: list[list[str]], left_cols: int = 1) -> str:
    divider = [":--" if i < left_cols else "--:" for i in range(len(headers))]
    return "\n".join(
        ["| " + " | ".join(headers) + " |", "| " + " | ".join(divider) + " |"]
        + ["| " + " | ".join(row) + " |" for row in rows]
    )


def paper_table(summary: dict) -> str:
    published = summary["paper_ktester_published"]
    kt, fu = summary["ktester_arm"], summary["fuse_arm"]
    rows = []
    for label, key, paper_key, unit in PAPER_METRICS:
        # Aggregated coverage/MS columns are already in percent; CPR/EPR are rates.
        already = unit == "pct"
        kt_value = fmt(kt.get(key), unit, already)
        fu_value = fmt(fu.get(key), unit, already)
        paper_value = fmt(published.get(paper_key), unit) if paper_key else "—"
        if isinstance(kt.get(key), (int, float)) and isinstance(fu.get(key), (int, float)):
            raw = fu[key] - kt[key]
            scale = 100 if unit == "rate" else 1
            suffix = " pp" if unit in {"rate", "pct"} else ""
            change = f"{raw * scale:+.2f}{suffix}"
        else:
            change = "—"
        rows.append([label, paper_value, kt_value, fu_value, change])
    return markdown(
        ["Metric", "KTester (paper)", "Arm K (đo được)", "Arm F (fused)", "Δ F−K"], rows
    )


def significance_block(summary: dict) -> list[str]:
    lines = ["### Kiểm định cặp (Wilcoxon signed-rank)", ""]
    for label, key in (
        ("Mutation score", "mutation_score"),
        ("Line coverage", "line_coverage"),
        ("Branch coverage", "branch_coverage"),
    ):
        test = summary["significance"][key]
        if "p_value" in test:
            lines.append(
                f"- **{label}**: p = {test['p_value']:.4g}, "
                f"effect size (rank-biserial) = {test['effect_size_rank_biserial']:+.3f}, "
                f"{test['improved']} tăng / {test['regressed']} giảm "
                f"trên {test['n_nonzero']} cặp khác 0"
            )
        else:
            lines.append(f"- **{label}**: {test.get('note', 'không tính được')} "
                         f"({test['improved']} tăng / {test['regressed']} giảm)")
    return lines


def mutator_table(summary: dict) -> str:
    rows = []
    for mutator, data in summary["by_mutator"].items():
        rows.append([
            mutator.removesuffix("Mutator"),
            str(data["kt_total"]),
            f"{data['kt_ms']:.2f}%" if data["kt_ms"] is not None else "—",
            f"{data['f_ms']:.2f}%" if data["f_ms"] is not None else "—",
            f"{data['delta_pp']:+.2f} pp" if data["delta_pp"] is not None else "—",
        ])
    return markdown(["Mutation operator", "Mutants", "Arm K MS", "Arm F MS", "Δ"], rows)


def render(args: argparse.Namespace) -> int:
    all_rows = collect_rows()
    summary = aggregate(all_rows)

    rows = [r for r in all_rows if r["status"] == "COMPLETED"] if args.only_completed else all_rows
    for row in rows:
        delta = row.get("d_ms_pp")
        row["winner"] = (
            ("fuse" if delta > 0 else "ktester" if delta < 0 else "tie")
            if isinstance(delta, (int, float)) else ""
        )
        if args.with_scratch:
            row["scratch_ms"] = scratch_score(row["task_id"])
    columns = list(TASK_COLUMNS)
    if args.with_scratch:
        columns.insert(-1, ("scratch_ms", "Scratch MS%"))

    reverse = args.sort_by not in {"task_id", "project"}
    rows.sort(
        key=lambda row: (row.get(args.sort_by) if isinstance(
            row.get(args.sort_by), (int, float)) else (-1e9 if reverse else "")),
        reverse=reverse,
    )

    body = [
        "# KTester + MutGen fusion — toàn bộ metric của hai paper",
        "",
        f"- Run root: `{config.RUN_ROOT}`",
        f"- Arm K = test class KTester; Arm F = chính class đó sau khi MutGen thêm test",
        f"- Stage A (seed): `{config.KTESTER_SOURCE}` | Stage B: `{config.MUTGEN_MODEL}`",
        f"- Mọi metric scoped đúng focal method (JaCoCo theo overload, PIT theo dòng)",
        "",
    ]
    if not summary.get("completed"):
        body += ["Chưa có task nào COMPLETED.", ""]
    else:
        m = summary["mutants"]
        outcome = summary["per_method_outcome"]
        body += [
            f"**{summary['completed']}/{summary['selected']} focal method hoàn tất.** "
            f"Mutant: {m['killed_ktester']} → {m['killed_fuse']} / {m['total']} bị kill "
            f"(+{m['newly_killed']} mới, −{m['lost_kills']} mất). "
            f"Thắng-thua theo method: fuse {outcome['fuse_wins']}, "
            f"ktester {outcome['ktester_wins']}, hoà {outcome['ties']}.",
            "",
            "## 1. Metric paper",
            "",
            "Cột *KTester (paper)* là số công bố trong paper KTester (gpt-4o-mini, "
            "toàn dataset) — mốc tham chiếu, không phải so sánh cặp. Hai cột sau mới "
            "là so sánh cặp trên cùng workspace.",
            "",
            paper_table(summary),
            "",
            *significance_block(summary),
            "",
            "## 2. Theo mutation operator (RQ2)",
            "",
            mutator_table(summary),
            "",
        ]
    body += [
        "## 3. Theo từng focal method",
        "",
        markdown(
            [label for _, label in columns],
            [[str(row.get(key, "")) if row.get(key, "") != "" else "—"
              for key, _ in columns] for row in rows],
            left_cols=2,
        ),
        "",
    ]

    text = "\n".join(body)
    config.RUN_ROOT.mkdir(parents=True, exist_ok=True)
    output = config.RUN_ROOT / "results_table.md"
    output.write_text(text, encoding="utf-8")
    mb.write_json(config.RUN_ROOT / "paper_metrics.json", summary)
    print(text)
    print(f"\nĐã ghi: {output} và paper_metrics.json")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--only-completed", action="store_true")
    parser.add_argument("--with-scratch", action="store_true",
                        help="thêm cột MutGen-from-scratch từ run cũ")
    parser.add_argument("--sort-by", default="d_ms_pp",
                        choices=["d_ms_pp", "d_lc_pp", "d_bc_pp", "newly_killed",
                                 "f_ms", "kt_ms", "mutants", "task_id", "project"])
    return render(parser.parse_args())


if __name__ == "__main__":
    raise SystemExit(main())
