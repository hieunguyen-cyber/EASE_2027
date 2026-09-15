"""Stage B -- grow the KTester test class with mutation-guided tests.

The existing MUTGEN driver points the generator at an empty scaffold, so the
model has to rediscover how to construct the focal class before it can kill
anything.  Here the generator starts from KTester's test class instead, and the
feedback describes only the mutants that class failed to kill.  MutGen's
`validate_unittest` merges each accepted test into the file it was given, so the
two techniques compose into a single suite rather than two competing ones.

Two failure modes distinguish this from the from-scratch setup and are handled
explicitly:

1. MutGen validates a candidate by running `mvn -Dtest=<class> test` and reads
   the exit code.  A seed that is already red would make every candidate look
   broken, so the seed is made green before generation (see `greenify_seed`).
2. A surviving mutant and an uncovered mutant call for different tests -- a
   sharper assertion versus a new input path.  The feedback separates them.
"""

from __future__ import annotations

import contextlib
import sys
import traceback
from pathlib import Path

import config
from pipeline import metrics
from pipeline import mutgen_bridge as mb


def existing_test_methods(test_file: Path) -> list[str]:
    """Names of the test methods already present in the seed class."""
    return metrics.test_method_names(test_file)


def greenify_seed(
    build_dir: Path,
    seed_files: list[Path],
    test_class: str,
    maven: str,
    logs_dir: Path,
    compile_pass: bool,
    execution_pass: bool,
) -> dict:
    """Make the seed suite pass, disabling only the methods that genuinely fail.

    Call this straight after `metrics.measure_arm`, which has already run the
    suite with failures ignored -- the Surefire reports it left behind say which
    methods failed, so no extra test run is needed to find out.

    A red seed poisons MutGen's accept/reject signal: it validates a candidate
    by the exit code of `mvn -Dtest=<class> test`, so every new test would look
    broken. A seed that does not compile cannot be fused at all. Both outcomes
    are reported rather than silently worked around.
    """
    if not compile_pass:
        return {"status": "SEED_UNCOMPILABLE", "disabled_methods": [], "test_count": 0}

    failures_by_class, total = mb.surefire_failures(build_dir, [test_class])
    if execution_pass and not failures_by_class:
        return {"status": "GREEN", "disabled_methods": [], "test_count": total}

    disabled: list[str] = []
    for path in seed_files:
        disabled.extend(
            mb.disable_test_methods(path, failures_by_class.get(test_class, set()))
        )
    disabled = sorted(set(disabled))
    confirm = mb.run_command(
        mb.maven_test_command(maven, test_class),
        build_dir,
        logs_dir / "b2_seed_test_filtered.log",
        "seed-test-filtered",
        check=False,
    )
    _, total = mb.surefire_failures(build_dir, [test_class])
    if confirm.returncode != 0:
        tail = (confirm.stdout + "\n" + confirm.stderr).splitlines()[-40:]
        return {
            "status": "SEED_STILL_RED",
            "disabled_methods": disabled,
            "test_count": total,
            "error": "\n".join(tail),
        }
    return {
        "status": "GREEN_FILTERED",
        "disabled_methods": disabled,
        "test_count": total,
    }


def build_fuse_feedback(
    mutants: list[dict],
    source_file: Path,
    existing_tests: list[str],
    limit: int,
) -> str:
    """Prompt block describing what the KTester suite left alive."""
    source_lines = source_file.read_text(
        encoding="utf-8", errors="replace"
    ).splitlines()

    def describe(mutant: dict) -> str:
        line_number = mutant["line"]
        source = (
            source_lines[line_number - 1].strip()
            if 0 < line_number <= len(source_lines)
            else ""
        )
        return (
            f"- line {line_number} | {mutant['mutator']} | "
            f"{mutant['description']} | source: {source}"
        )

    survived = [m for m in mutants if m["status"] == "SURVIVED"]
    uncovered = [m for m in mutants if m["status"] == "NO_COVERAGE"]
    other = [
        m for m in mutants
        if not m["detected"] and m["status"] not in {"SURVIVED", "NO_COVERAGE"}
    ]

    budget = max(limit, 1)
    survived_shown = survived[:budget]
    remaining = max(budget - len(survived_shown), 0)
    uncovered_shown = uncovered[:remaining]

    block = [
        "An existing JUnit 5 test class for this focal method is already in place.",
        "PIT was run against it. The mutants below are the ones it did NOT kill.",
        "Add new test methods that kill them. Do not rewrite or delete existing tests.",
        "",
    ]
    if survived_shown:
        block.append(
            f"SURVIVED ({len(survived)} total) -- the mutated line already executes, "
            "but no assertion distinguishes the mutant from the original. "
            "Assert on the specific value or side effect this line changes:"
        )
        block.extend(describe(m) for m in survived_shown)
        if len(survived) > len(survived_shown):
            block.append(f"- ... and {len(survived) - len(survived_shown)} more SURVIVED mutants.")
        block.append("")
    if uncovered_shown:
        block.append(
            f"NO_COVERAGE ({len(uncovered)} total) -- no existing test reaches this line. "
            "Choose inputs that drive execution into this branch, then assert on the result:"
        )
        block.extend(describe(m) for m in uncovered_shown)
        if len(uncovered) > len(uncovered_shown):
            block.append(f"- ... and {len(uncovered) - len(uncovered_shown)} more NO_COVERAGE mutants.")
        block.append("")
    if other:
        block.append(f"Other undetected statuses: {sorted({m['status'] for m in other})}")
        block.append("")
    if not survived and not uncovered and not other:
        block.append("The existing suite already kills every focal mutant.")
        block.append("")
    if existing_tests:
        block.append(
            "Test methods that already exist -- pick different names to avoid a "
            "duplicate-method compilation error:"
        )
        block.append("  " + ", ".join(existing_tests))
    return "\n".join(block).rstrip() + "\n"


def fuse_description(task: dict, seed_test_count: int) -> str:
    return (
        "This is a repository-level KTester task. A KTester-generated JUnit 5 test "
        f"class with {seed_test_count} test method(s) is already present in the test "
        "file shown below. Your job is to ADD test methods to that same class so that "
        "the PIT mutants listed in the mutation feedback are killed. Keep the existing "
        "setup/teardown and existing tests untouched. Generate tests only for the focal "
        f"method {task['class']}.{task['method-name']}.\n\nFocal method:\n"
        + task["focal-method"]
    )


def invoke_mutgen_on_seed(
    build_dir: Path,
    task: dict,
    feedback: str,
    description: str,
    maven: str,
    log_path: Path,
) -> str | None:
    """Run one MutGen generation pass against the seeded test class."""
    if str(config.MUTGEN_ROOT) not in sys.path:
        sys.path.insert(0, str(config.MUTGEN_ROOT))
    from mutahunter.core.analyzer import Analyzer
    from mutahunter.core.coverage_processor import CoverageProcessor
    from mutahunter.core.entities.config import UnittestGeneratorLineConfig
    from mutahunter.core.prompt_factory import TestGenerationPromptFactory
    from mutahunter.core.router import LLMRouter
    from mutahunter.core.unit_test_gen import UnittestGenLine

    generator_config = UnittestGeneratorLineConfig(
        model=config.MUTGEN_MODEL,
        api_base=config.OLLAMA_SERVER_URL,
        test_file_path=task["test-path"],
        source_file_path=task["source-path"],
        test_command=(
            f"{maven} -q {' '.join(mb.SKIP_FLAGS)} -Dtest={task['test-class']} test"
        ),
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
        config=generator_config,
        coverage_processor=CoverageProcessor(
            coverage_type="jacoco",
            code_coverage_report_path=generator_config.code_coverage_report_path,
        ),
        analyzer=Analyzer(),
        router=LLMRouter(
            model=config.MUTGEN_MODEL, api_base=config.OLLAMA_SERVER_URL
        ),
        prompt=TestGenerationPromptFactory.get_prompt(),
    )

    error = None
    log_path.parent.mkdir(parents=True, exist_ok=True)
    with log_path.open("w", encoding="utf-8") as handle:
        tee_out, tee_err = mb.Tee(sys.stdout, handle), mb.Tee(sys.stderr, handle)
        try:
            with mb.working_directory(build_dir), contextlib.redirect_stdout(
                tee_out
            ), contextlib.redirect_stderr(tee_err):
                generator.run()
        except Exception:
            error = traceback.format_exc()
            handle.write("\n[FUSE STAGE-B ERROR]\n" + error)
    return error
