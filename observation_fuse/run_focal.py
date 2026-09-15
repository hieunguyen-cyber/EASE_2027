#!/usr/bin/env python3
"""Run the KTester+MutGen fusion on one focal method.

    prepare  copy the project into an isolated workspace and install the seed
    run      score the seed, generate mutation-guided tests, score the fusion
    status   print what a prepared/finished run contains

`prepare` is offline.  `run` is the expensive half: two or three PIT runs plus
one MutGen generation pass per round.
"""

from __future__ import annotations

import argparse
import shutil
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import config
from pipeline import fuse_stage, ktester_stage, metrics
from pipeline import mutgen_bridge as mb


def task_dir(task_id: str) -> Path:
    return config.TASKS_ROOT / task_id


def mutant_key(mutant: dict) -> tuple:
    """Stable identity for a mutant across two PIT runs of the same class.

    `focal_mutations` numbers mutants by their position in the report, which
    shifts when the test suite changes, so the index cannot be used here.
    """
    return (
        mutant["line"],
        mutant["mutator"],
        mutant["description"],
        mutant["method_description"],
    )


# =========================================================================
# prepare
# =========================================================================
def prepare(args: argparse.Namespace) -> int:
    project_key, task = mb.find_task(config.DATASET, args.task_id)
    project_name = task.get("project-name", project_key)
    source_project = config.PROJECTS_ROOT / project_name
    if not (source_project / "pom.xml").is_file():
        raise SystemExit(f"Không tìm thấy Maven project: {source_project}")
    source_module = mb.resolve_module_root(source_project, task)
    module_relative = source_module.relative_to(source_project)

    run_dir = task_dir(args.task_id)
    workspace = run_dir / "workspace"
    build_dir = workspace / module_relative
    manifest_path = run_dir / "manifest.json"

    if manifest_path.exists() and not args.force:
        manifest = mb.read_json(manifest_path)
        print(f"Đã chuẩn bị sẵn: {run_dir} (state={manifest.get('state')})")
        return 0
    if run_dir.exists():
        shutil.rmtree(run_dir)

    print(f"Copy {project_name} -> workspace cô lập...")
    run_dir.mkdir(parents=True)
    shutil.copytree(
        source_project,
        workspace,
        ignore=shutil.ignore_patterns(".git", "target", ".idea", ".vscode", "libs"),
    )

    # The project's own tests are moved aside: PIT is scoped to the focal
    # method, and an unrelated red test would block every Maven invocation.
    active_tests = build_dir / "src/test"
    disabled_tests = build_dir / "src/ktester-test-disabled"
    if active_tests.exists():
        active_tests.rename(disabled_tests)
    disabled_resources = disabled_tests / "resources"
    if disabled_resources.is_dir():
        shutil.copytree(
            disabled_resources, build_dir / "src/test/resources", dirs_exist_ok=True
        )

    seed_sources = ktester_stage.released_test_sources(source_module, task)
    seed_dir = run_dir / "seed"
    seed_dir.mkdir()
    seed_status = "missing"
    installed: list[Path] = []
    if config.KTESTER_SOURCE == "artifact":
        if not seed_sources:
            seed_status = "missing"
        else:
            for source in seed_sources:
                shutil.copy2(source, seed_dir / source.name)
            installed = ktester_stage.install_seed(seed_sources, source_module, build_dir)
            seed_status = "installed"
    else:
        seed_status = "deferred-to-run"

    mb.add_pitest_plugin(build_dir / "pom.xml")
    mb.ensure_maven_wrapper(build_dir, config.PROJECTS_ROOT)

    manifest = {
        "schema_version": 1,
        "state": "prepared",
        "prepared_at": mb.utc_now(),
        "task_id": args.task_id,
        "project": project_name,
        "source_project": str(source_project),
        "workspace": str(workspace),
        "module_relative": str(module_relative),
        "build_dir": str(build_dir),
        "required_java_major": mb.required_java_major(project_name),
        "dataset": str(config.DATASET),
        "task": task,
        "stage_a": {
            "source": config.KTESTER_SOURCE,
            "seed_status": seed_status,
            "seed_files": [str(path) for path in seed_sources],
            "installed": [str(path) for path in installed],
            "model": config.KTESTER_LLM_MODEL if config.KTESTER_SOURCE == "generate" else None,
        },
        "stage_b": {
            "model": config.MUTGEN_MODEL,
            "server": config.OLLAMA_SERVER_URL,
            "rounds": config.FUSE_ROUNDS,
            "max_feedback_mutants": config.MAX_FEEDBACK_MUTANTS,
        },
        "design": {
            "seed": "KTester test class for this focal method",
            "arm_ktester": "PIT on the seed alone",
            "arm_fuse": "PIT on the seed after MutGen adds mutation-guided tests",
            "scored_scope": "focal method only",
            "project_own_tests_on_classpath": False,
        },
        "isolation": {
            "disabled_project_tests": str(disabled_tests),
            "fused_test": str(build_dir / task["test-path"]),
        },
    }
    mb.write_json(manifest_path, manifest)
    print(f"Chuẩn bị xong: {run_dir}")
    print(f"  seed: {seed_status} ({len(seed_sources)} file)")
    print(f"  chạy: python3 run_focal.py run --task-id {args.task_id}")
    return 0


# =========================================================================
# run
# =========================================================================
def run(args: argparse.Namespace) -> int:
    run_dir = task_dir(args.task_id)
    manifest_path = run_dir / "manifest.json"
    if not manifest_path.exists():
        raise SystemExit(
            f"Chưa prepare. Chạy: python3 run_focal.py prepare --task-id {args.task_id}"
        )
    manifest = mb.read_json(manifest_path)
    if manifest.get("state") == "completed" and not args.allow_rerun:
        raise SystemExit(
            "Task này đã completed. Dùng --allow-rerun nếu chủ động muốn chạy lại "
            "trên test hiện tại (kết quả sẽ không còn tái lập được)."
        )

    task = manifest["task"]
    build_dir = Path(manifest["build_dir"])
    results_dir = run_dir / "results"
    logs_dir = run_dir / "logs"
    results_dir.mkdir(exist_ok=True)
    logs_dir.mkdir(exist_ok=True)
    source_file = build_dir / task["source-path"]
    test_class = task["test-class"]
    seed_file = build_dir / task["test-path"]

    print("Preflight: Java, Maven, Ollama...")
    environment = mb.preflight_fuse(
        build_dir, int(manifest.get("required_java_major", 11))
    )
    maven = environment["maven_path"]
    manifest.update(
        {"state": "running", "started_at": mb.utc_now(), "environment": environment}
    )
    mb.write_json(manifest_path, manifest)

    summary = {
        "task_id": args.task_id,
        "project": manifest["project"],
        "focal_class": task["class"],
        "focal_method": task["method-name"],
        "stage_a": dict(manifest["stage_a"]),
        "stage_b": dict(manifest["stage_b"]),
        "started_at": manifest["started_at"],
        "paper_ktester_reference": mb.PAPER_KTESTER,
    }

    def finish(status: str, code: int, message: str) -> int:
        summary["status"] = status
        summary["completed_at"] = mb.utc_now()
        mb.write_json(results_dir / "summary.json", summary)
        manifest["state"] = "completed" if code == 0 else "failed"
        mb.write_json(manifest_path, manifest)
        print(message)
        return code

    try:
        mb.run_command(
            [maven, "-q", *mb.SKIP_FLAGS, "-DskipTests", "compile"],
            build_dir,
            logs_dir / "00_compile.log",
            "compile",
        )

        # ---------------- Stage A: obtain the seed ----------------
        stage_a_started = time.monotonic()
        if manifest["stage_a"]["source"] == "generate":
            print("\n[STAGE A] Sinh test class bằng pipeline KTester...")
            produced = ktester_stage.generate_seed(
                config.RUN_ROOT, run_dir, manifest["project"], task,
                Path(manifest["workspace"]), build_dir, maven, logs_dir,
            )
            for path in produced:
                shutil.copy2(path, run_dir / "seed" / path.name)
            manifest["stage_a"]["seed_status"] = "generated"
            summary["stage_a"]["seed_status"] = "generated"
        stage_a_seconds = round(time.monotonic() - stage_a_started, 3)
        summary["stage_a"]["seconds"] = stage_a_seconds

        if not seed_file.is_file():
            return finish("SEED_MISSING", 1, "Không có KTester seed cho task này.")
        shutil.copyfile(seed_file, results_dir / "seed_test.java")

        # ---------------- Arm K: KTester paper metrics ----------------
        print("\n[ARM K] Đo CPR/EPR/coverage trên seed nguyên bản...")
        arm_k = metrics.measure_arm(
            build_dir, task, maven, logs_dir, "ktester", results_dir
        )
        arm_k.update(metrics.passed_only(arm_k))
        arm_k["generation_seconds"] = stage_a_seconds if stage_a_seconds > 0.5 else None

        seed_health = fuse_stage.greenify_seed(
            build_dir, [seed_file], test_class, maven, logs_dir,
            arm_k["compile_pass"], arm_k["execution_pass"],
        )
        summary["seed_health"] = seed_health
        summary["ktester"] = arm_k
        if seed_health["status"] in {"SEED_UNCOMPILABLE", "SEED_STILL_RED"}:
            return finish(
                seed_health["status"], 1,
                f"Seed không dùng làm nền được ({seed_health['status']}). "
                "Xem logs/m1_ktester_compile.log và logs/b2_seed_test_filtered.log",
            )
        print(
            f"Seed: {seed_health['status']}, {seed_health['test_count']} test chạy, "
            f"{len(seed_health['disabled_methods'])} test bị disable | "
            f"CPR={arm_k['compile_pass']} EPR={arm_k['execution_pass']} "
            f"LC={arm_k['line_coverage']} BC={arm_k['branch_coverage']}"
        )

        # ---------------- Arm K: mutation score ----------------
        mb.run_command(
            mb.pit_command(task, maven), build_dir,
            logs_dir / "b3_seed_pit.log", "seed-pit",
        )
        seed_report = results_dir / "ktester_mutations.xml"
        shutil.copy2(mb.newest_mutation_report(build_dir), seed_report)
        seed_mutants = mb.focal_mutations(seed_report, task, source_file)
        if not seed_mutants:
            raise RuntimeError(
                "PIT không sinh mutant nào cho focal method; kiểm tra signature trong dataset."
            )
        seed_stats = mb.mutation_stats(seed_mutants)
        mb.write_json(results_dir / "ktester_focal_mutants.json", {"mutants": seed_mutants})
        arm_k["stats"] = seed_stats
        arm_k["kills_per_test"] = metrics.kills_per_test(seed_stats, arm_k["test_cases"])
        arm_k["by_mutator"] = metrics.mutator_breakdown(seed_mutants)
        print(
            f"[ARM K] MS = {seed_stats['detected']}/{seed_stats['total']} "
            f"({(seed_stats['mutation_score'] or 0):.3f})"
        )

        # ---------------- Arm F: mutation-guided augmentation ----------------
        rounds = []
        current_mutants = seed_mutants
        generation_error = None
        stage_b_started = time.monotonic()
        for round_index in range(1, int(manifest["stage_b"]["rounds"]) + 1):
            existing = fuse_stage.existing_test_methods(seed_file)
            feedback = fuse_stage.build_fuse_feedback(
                current_mutants, source_file, existing,
                int(manifest["stage_b"]["max_feedback_mutants"]),
            )
            (results_dir / f"mutation_feedback_r{round_index}.txt").write_text(
                feedback, encoding="utf-8"
            )
            undetected = sum(1 for m in current_mutants if not m["detected"])
            if undetected == 0:
                print(f"[ARM F] Round {round_index}: seed đã kill hết mutant, dừng.")
                break
            print(
                f"\n[ARM F] Round {round_index}: {undetected} mutant chưa bị kill, "
                f"gọi {config.MUTGEN_MODEL}..."
            )
            round_started = time.monotonic()
            error = fuse_stage.invoke_mutgen_on_seed(
                build_dir, task, feedback,
                fuse_stage.fuse_description(task, len(existing)),
                maven, logs_dir / f"b4_mutgen_r{round_index}.log",
            )
            generation_error = generation_error or error
            if error:
                print(f"[ARM F] Round {round_index} lỗi generation; xem log.")
            rounds.append({
                "round": round_index,
                "undetected_before": undetected,
                "existing_tests_before": len(existing),
                "tests_after": metrics.count_test_methods(seed_file),
                "seconds": round(time.monotonic() - round_started, 3),
                "generation_error": bool(error),
            })
            if round_index < int(manifest["stage_b"]["rounds"]):
                mb.run_command(
                    mb.pit_command(task, maven), build_dir,
                    logs_dir / f"b5_interim_pit_r{round_index}.log",
                    f"interim-pit-r{round_index}",
                )
                current_mutants = mb.focal_mutations(
                    mb.newest_mutation_report(build_dir), task, source_file
                )
        stage_b_seconds = round(time.monotonic() - stage_b_started, 3)
        summary["rounds"] = rounds
        summary["generation_error"] = generation_error
        summary["stage_b"]["seconds"] = stage_b_seconds

        shutil.copyfile(seed_file, results_dir / "fused_test.java")

        # ---------------- Arm F: KTester paper metrics ----------------
        print("\n[ARM F] Đo CPR/EPR/coverage trên suite đã fuse...")
        arm_f = metrics.measure_arm(
            build_dir, task, maven, logs_dir, "fused", results_dir
        )
        # The fused artifact inherits whatever the seed had disabled, so the
        # strict reading counts those as an execution failure exactly as arm K
        # did; the lenient one asks only whether the added tests are green.
        arm_f["execution_pass_new_tests_only"] = arm_f["execution_pass"]
        arm_f["execution_pass"] = arm_f["execution_pass"] and not seed_health["disabled_methods"]
        arm_f.update(metrics.passed_only(arm_f))
        arm_f["generation_seconds"] = stage_b_seconds
        arm_f["inherited_disabled_methods"] = len(seed_health["disabled_methods"])

        # ---------------- Arm F: mutation score ----------------
        mb.run_command(
            mb.pit_command(task, maven), build_dir,
            logs_dir / "b7_fused_pit.log", "fused-pit",
        )
        fused_report = results_dir / "fused_mutations.xml"
        shutil.copy2(mb.newest_mutation_report(build_dir), fused_report)
        fused_mutants = mb.focal_mutations(fused_report, task, source_file)
        fused_stats = mb.mutation_stats(fused_mutants)
        mb.write_json(results_dir / "fused_focal_mutants.json", {"mutants": fused_mutants})
        arm_f["stats"] = fused_stats
        arm_f["kills_per_test"] = metrics.kills_per_test(fused_stats, arm_f["test_cases"])
        arm_f["by_mutator"] = metrics.mutator_breakdown(fused_mutants)
        summary["fuse"] = arm_f

        # ---------------- Paired deltas ----------------
        killed_before = {mutant_key(m) for m in seed_mutants if m["detected"]}
        killed_after = {mutant_key(m) for m in fused_mutants if m["detected"]}
        newly_killed = sorted(killed_after - killed_before)
        lost = sorted(killed_before - killed_after)

        delta = metrics.paired_delta(
            arm_f, arm_k,
            ["line_coverage", "branch_coverage", "instruction_coverage",
             "line_coverage_passed", "branch_coverage_passed",
             "instruction_coverage_passed", "kills_per_test"],
        )
        delta["mutation_score"] = round(
            (fused_stats["mutation_score"] or 0) - (seed_stats["mutation_score"] or 0), 6
        )
        delta["mutation_score_points"] = round(delta["mutation_score"] * 100, 3)
        delta["newly_killed"] = len(newly_killed)
        delta["lost_kills"] = len(lost)
        delta["added_test_methods"] = arm_f["test_cases"] - arm_k["test_cases"]
        delta["compile_pass"] = (arm_f["compile_pass"], arm_k["compile_pass"])
        delta["execution_pass"] = (arm_f["execution_pass"], arm_k["execution_pass"])
        delta["newly_killed_mutants"] = [
            {"line": key[0], "mutator": key[1], "description": key[2]}
            for key in newly_killed
        ]
        delta["lost_kill_mutants"] = [
            {"line": key[0], "mutator": key[1], "description": key[2]} for key in lost
        ]
        summary["delta"] = delta

        print(
            f"[ARM F] MS = {fused_stats['detected']}/{fused_stats['total']} "
            f"({(fused_stats['mutation_score'] or 0):.3f}) | "
            f"LC={arm_f['line_coverage']} BC={arm_f['branch_coverage']}"
        )
        print(
            f"[DELTA] MS {delta['mutation_score_points']:+.2f} pp | "
            f"LC {(delta['line_coverage'] or 0) * 100:+.2f} pp | "
            f"BC {(delta['branch_coverage'] or 0) * 100:+.2f} pp | "
            f"+{delta['newly_killed']} kill | +{delta['added_test_methods']} test"
        )
        if lost:
            print(f"[DELTA] Cảnh báo: mất {len(lost)} kill so với seed.")
        if not args.keep_build_output:
            shutil.rmtree(build_dir / "target", ignore_errors=True)
        return finish("COMPLETED", 0, "")

    except Exception as error:  # noqa: BLE001 - recorded, then reported as exit code
        summary["error"] = str(error)
        print(f"ERROR: {error}", file=sys.stderr)
        return finish("FAILED", 1, "")


# =========================================================================
# status
# =========================================================================
def status(args: argparse.Namespace) -> int:
    run_dir = task_dir(args.task_id)
    manifest_path = run_dir / "manifest.json"
    if not manifest_path.exists():
        print(f"Chưa prepare: {run_dir}")
        return 1
    manifest = mb.read_json(manifest_path)
    print(f"task    : {manifest['task_id']} ({manifest['project']})")
    print(f"state   : {manifest['state']}")
    print(f"stage A : {manifest['stage_a']['source']} / {manifest['stage_a']['seed_status']}")
    summary_path = run_dir / "results/summary.json"
    if not summary_path.exists():
        print("chưa có results/summary.json")
        return 0
    summary = mb.read_json(summary_path)
    print(f"status  : {summary.get('status')}")
    if summary.get("status") == "COMPLETED":
        k = summary["ktester"]["stats"]
        f = summary["fuse"]["stats"]
        print(f"KTester : {k['detected']}/{k['total']}  MS={k['mutation_score']}")
        print(f"Fuse    : {f['detected']}/{f['total']}  MS={f['mutation_score']}")
        print(f"Delta   : {summary['delta']['mutation_score_points']:+.2f} pp")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    p_prepare = sub.add_parser("prepare", help="tạo workspace cô lập + cài seed")
    p_prepare.add_argument("--task-id", required=True)
    p_prepare.add_argument("--force", action="store_true", help="xoá và prepare lại")
    p_prepare.set_defaults(handler=prepare)

    p_run = sub.add_parser("run", help="chấm seed, sinh test theo mutant, chấm lại")
    p_run.add_argument("--task-id", required=True)
    p_run.add_argument("--allow-rerun", action="store_true")
    p_run.add_argument("--keep-build-output", action="store_true")
    p_run.set_defaults(handler=run)

    p_status = sub.add_parser("status", help="xem trạng thái một task")
    p_status.add_argument("--task-id", required=True)
    p_status.set_defaults(handler=status)
    return parser


def main() -> int:
    args = build_parser().parse_args()
    try:
        return args.handler(args)
    except (RuntimeError, OSError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
