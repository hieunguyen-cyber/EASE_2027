"""Every metric both papers report, computed per arm and paired across arms.

KTester's paper measures how usable a generated suite is and how much of the
focal method it exercises:

    CPR  compile pass rate        EPR  execution pass rate
    LC   line coverage           BC   branch coverage
    IC   instruction coverage    (KTester's own code reports instruction)
    LCP/BCP/ICP                  the same, counted as 0 when the suite is red
    AvTC average test cases      AvT  average generation seconds

MutGen's paper measures how discriminating the suite is:

    MS   mutation score = detected / total, scoped to the focal method
    killed / survived / no_coverage breakdown
    kills per test
    per-mutation-operator score (the paper's RQ2 view)

The published KTester numbers travel with the code as `mb.PAPER_KTESTER`, so a
run can be read against the paper without hunting for the table.

Coverage is scoped to the exact focal-method overload -- JaCoCo reports one
`<method>` node per overload, matched by name plus declaration line.
"""

from __future__ import annotations

import shutil
import xml.etree.ElementTree as ET
from pathlib import Path

from pipeline import mutgen_bridge as mb

COUNTERS = {"LINE": "line", "BRANCH": "branch", "INSTRUCTION": "instruction"}

# PIT's DEFAULTS group, as named in the MutGen paper's operator analysis.
MUTATOR_ORDER = [
    "ConditionalsBoundaryMutator",
    "IncrementsMutator",
    "InvertNegsMutator",
    "MathMutator",
    "NegateConditionalsMutator",
    "VoidMethodCallMutator",
    "EmptyObjectReturnValsMutator",
    "BooleanFalseReturnValsMutator",
    "BooleanTrueReturnValsMutator",
    "NullReturnValsMutator",
    "PrimitiveReturnsMutator",
]


# =========================================================================
# Counting test methods
# =========================================================================
# MUTGEN's `count_test_methods` regex-matches "@Test" and so also counts
# annotations inside commented-out code. That is not a rare edge here: KTester's
# repair loop comments out test cases it cannot fix (`clean_error_cases`), so a
# released seed routinely carries commented "@Test" lines. Counting those
# inflates AvTC and deflates kills-per-test, so both are computed from the AST.
TEST_MARKERS = (b"@Test", b"@ParameterizedTest", b"@RepeatedTest")


def test_method_names(path: Path) -> list[str]:
    """Names of JUnit test methods actually declared in a Java test file."""
    if not path.is_file():
        return []
    from tree_sitter_languages import get_parser

    content = path.read_bytes()
    tree = get_parser("java").parse(content)
    names: list[str] = []

    # tree-sitter keeps a method's annotations inside its method_declaration
    # node, and puts commented-out code in comment nodes instead, so a marker
    # found in the node's own modifiers is a real annotation.
    def walk(node) -> None:
        if node.type == "method_declaration":
            name_node = node.child_by_field_name("name")
            modifiers = next(
                (child for child in node.children if child.type == "modifiers"), None
            )
            if name_node is not None and modifiers is not None:
                header = content[modifiers.start_byte : modifiers.end_byte]
                if any(marker in header for marker in TEST_MARKERS):
                    names.append(name_node.text.decode())
        for child in node.children:
            walk(child)

    walk(tree.root_node)
    return sorted(set(names))


def count_test_methods(path: Path) -> int:
    return len(test_method_names(path))


# =========================================================================
# Coverage (KTester protocol)
# =========================================================================
def focal_coverage(report_path: Path, task: dict, source_path: Path) -> dict:
    """LINE, BRANCH and INSTRUCTION counters for the focal-method overload."""
    root = ET.parse(report_path).getroot()
    xml_class = task["class"].replace(".", "/")
    expected = mb.method_name(task)
    start_line, end_line = mb.focal_line_range(source_path, task)

    matches = [
        method
        for class_node in root.findall(".//class")
        if class_node.attrib.get("name") == xml_class
        for method in class_node.findall("method")
        if method.attrib.get("name") == expected
        and start_line <= int(method.attrib.get("line", "0")) <= end_line
    ]
    if len(matches) != 1:
        found = [
            (m.attrib.get("name"), m.attrib.get("desc"), m.attrib.get("line"))
            for class_node in root.findall(".//class")
            if class_node.attrib.get("name") == xml_class
            for m in class_node.findall("method")
            if m.attrib.get("name") == expected
        ]
        raise RuntimeError(
            f"Cần đúng 1 method JaCoCo cho {task['class']}.{task['method-name']} "
            f"trong dòng {start_line}-{end_line}; tìm thấy {found}"
        )

    coverage: dict = {}
    for counter_type, prefix in COUNTERS.items():
        missed, covered = mb.jacoco_counter(matches[0], counter_type)
        coverage[f"{prefix}_missed"] = missed
        coverage[f"{prefix}_covered"] = covered
        coverage[f"{prefix}_coverage"] = mb.jacoco_ratio(covered, missed)
    return coverage


def zero_coverage() -> dict:
    coverage: dict = {}
    for prefix in COUNTERS.values():
        coverage[f"{prefix}_missed"] = 0
        coverage[f"{prefix}_covered"] = 0
        coverage[f"{prefix}_coverage"] = 0.0
    return coverage


def measure_arm(
    build_dir: Path,
    task: dict,
    maven: str,
    logs_dir: Path,
    label: str,
    results_dir: Path | None = None,
) -> dict:
    """Run the KTester measurement protocol against whatever test class is installed.

    Failures are ignored during the coverage run (`-Dmaven.test.failure.ignore`)
    so a partially red suite still reports the coverage it achieved -- that is
    how KTester reports LC/BC -- while EPR records that it was red.
    """
    test_class = task["test-class"]
    test_file = build_dir / task["test-path"]
    pom_path = build_dir / "pom.xml"
    declared = count_test_methods(test_file)
    row: dict = {
        "test_cases": declared,
        "compile_pass": False,
        "execution_pass": False,
        "tests_executed": 0,
        "tests_failed": 0,
        **zero_coverage(),
    }

    original_pom = mb.enable_jacoco_argline(pom_path)
    try:
        base = [maven, "-q", *mb.SKIP_FLAGS, f"-Dtest={test_class}"]
        compiled = mb.run_command(
            base + ["clean", "test-compile"],
            build_dir,
            logs_dir / f"m1_{label}_compile.log",
            f"{label}-compile",
            check=False,
        )
        row["compile_pass"] = compiled.returncode == 0
        if not row["compile_pass"]:
            return row

        # Several subjects (commons-collections, gson, ...) already bind the
        # JaCoCo agent in their own pom. Injecting a second `prepare-agent`
        # attaches two agents writing one destfile and the forked VM dies with
        # exit 134, so only inject when the project produced no exec file.
        report_goal = f"org.jacoco:jacoco-maven-plugin:{mb.JACOCO_VERSION}:report"
        tested = mb.run_command(
            base + ["-Dmaven.test.failure.ignore=true", "test"],
            build_dir,
            logs_dir / f"m2_{label}_test.log",
            f"{label}-test",
            check=False,
        )
        exec_file = build_dir / "target/jacoco.exec"
        if exec_file.is_file() and exec_file.stat().st_size:
            mb.run_command(
                base + [report_goal],
                build_dir,
                logs_dir / f"m3_{label}_jacoco_report.log",
                f"{label}-jacoco-report",
                check=False,
            )
        else:
            mb.run_command(
                base
                + [
                    "-Dmaven.test.failure.ignore=true",
                    f"org.jacoco:jacoco-maven-plugin:{mb.JACOCO_VERSION}:prepare-agent",
                    "test",
                    report_goal,
                ],
                build_dir,
                logs_dir / f"m3_{label}_jacoco_agent.log",
                f"{label}-jacoco-agent",
                check=False,
            )
        executed, failed = mb.parse_surefire_counts(build_dir, test_class)
        row["tests_executed"] = executed
        row["tests_failed"] = failed
        row["execution_pass"] = (
            tested.returncode == 0 and declared > 0 and executed > 0 and failed == 0
        )

        report = build_dir / "target/site/jacoco/jacoco.xml"
        if not report.is_file():
            row["coverage_error"] = "JaCoCo XML report missing"
            return row
        row.update(focal_coverage(report, task, build_dir / task["source-path"]))
        if results_dir is not None:
            shutil.copy2(report, results_dir / f"{label}_jacoco.xml")
        return row
    finally:
        pom_path.write_bytes(original_pom)


def passed_only(row: dict) -> dict:
    """LCP/BCP/ICP: coverage counted as zero unless the suite executed cleanly."""
    keep = bool(row.get("execution_pass"))
    return {
        f"{prefix}_coverage_passed": (row.get(f"{prefix}_coverage") or 0.0) if keep else 0.0
        for prefix in COUNTERS.values()
    }


# =========================================================================
# Mutation (MutGen protocol)
# =========================================================================
def mutator_breakdown(mutants: list[dict]) -> dict:
    """Per-operator kill rate -- the MutGen paper's RQ2 view.

    PIT already names the operator in the report, so this needs no model call.
    """
    buckets: dict[str, dict] = {}
    for mutant in mutants:
        bucket = buckets.setdefault(
            mutant["mutator"], {"total": 0, "detected": 0, "survived": 0, "no_coverage": 0}
        )
        bucket["total"] += 1
        bucket["detected"] += 1 if mutant["detected"] else 0
        bucket["survived"] += 1 if mutant["status"] == "SURVIVED" else 0
        bucket["no_coverage"] += 1 if mutant["status"] == "NO_COVERAGE" else 0
    for bucket in buckets.values():
        bucket["mutation_score"] = (
            round(bucket["detected"] / bucket["total"], 6) if bucket["total"] else None
        )

    def order(name: str) -> tuple:
        return (MUTATOR_ORDER.index(name) if name in MUTATOR_ORDER else len(MUTATOR_ORDER), name)

    return {name: buckets[name] for name in sorted(buckets, key=order)}


def kills_per_test(stats: dict, test_count: int) -> float | None:
    if not test_count:
        return None
    return round(stats["detected"] / test_count, 4)


# =========================================================================
# Paired statistics
# =========================================================================
def paired_delta(fuse: dict, seed: dict, keys: list[str]) -> dict:
    delta = {}
    for key in keys:
        a, b = fuse.get(key), seed.get(key)
        delta[key] = round(a - b, 6) if a is not None and b is not None else None
    return delta


def wilcoxon(before: list[float], after: list[float]) -> dict:
    """Wilcoxon signed-rank on the paired per-focal-method values.

    The samples are paired by construction -- same focal method, same workspace,
    same mutant set -- and the per-method differences are not normal, so the
    signed-rank test is the right one. Ties (delta == 0) are dropped, which is
    what `zero_method="wilcox"` does.
    """
    pairs = [
        (x, y) for x, y in zip(before, after)
        if x is not None and y is not None and x != y
    ]
    result = {
        "n_pairs": len(before),
        "n_nonzero": len(pairs),
        "improved": sum(1 for x, y in zip(before, after)
                        if x is not None and y is not None and y > x),
        "regressed": sum(1 for x, y in zip(before, after)
                         if x is not None and y is not None and y < x),
    }
    if len(pairs) < 6:
        result["note"] = "quá ít cặp khác 0 để kiểm định có ý nghĩa"
        return result
    try:
        from scipy.stats import wilcoxon as scipy_wilcoxon

        statistic, p_value = scipy_wilcoxon(
            [x for x, _ in pairs], [y for _, y in pairs], zero_method="wilcox"
        )
        result["statistic"] = float(statistic)
        result["p_value"] = float(p_value)
        # Matched-pairs rank-biserial correlation: the effect size that goes
        # with this test. +1 means every pair improved.
        deltas = [y - x for x, y in pairs]
        positives = sum(1 for d in deltas if d > 0)
        result["effect_size_rank_biserial"] = round(
            (2 * positives / len(deltas)) - 1, 4
        )
    except ImportError:
        result["note"] = "scipy chưa cài; bỏ qua kiểm định"
    return result
