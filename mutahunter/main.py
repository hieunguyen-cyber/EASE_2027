import argparse
import sys
from pathlib import Path

from mutahunter.core.analyzer import Analyzer
from mutahunter.core.controller import MutationTestController
from mutahunter.core.coverage_processor import CoverageProcessor
from mutahunter.core.db import MutationDatabase
from mutahunter.core.entities.config import (
    MutationTestControllerConfig,
    UnittestGeneratorLineConfig,
)
from mutahunter.core.io import FileOperationHandler
from mutahunter.core.llm_mutation_engine import LLMMutationEngine
from mutahunter.core.llm_backend import configure_backend
from mutahunter.core.prompt_factory import (
    MutationTestingPromptFactory,
    TestGenerationPromptFactory,
)
from mutahunter.core.report import MutantReport
from mutahunter.core.router import LLMRouter
from mutahunter.core.runner import MutantTestRunner
from mutahunter.core.unit_test_gen import UnittestGenLine
from mutahunter.core.paths import resolve_path


def add_mutation_testing_subparser(subparsers):
    parser = subparsers.add_parser("run", help="Run the mutation testing process.")
    parser.add_argument(
        "--workspace",
        type=Path,
        default=Path.cwd(),
        help="Project directory in which tests and coverage paths are resolved.",
    )
    parser.add_argument(
        "--test-command",
        type=str,
        default=None,
        required=True,
        help="The command to run the tests (e.g., 'pytest'). This argument is required.",
    )
    parser.add_argument(
        "--code-coverage-report-path",
        type=Path,
        required=False,
        help="The path to the code coverage report file. Optional.",
    )
    parser.add_argument(
        "--coverage-type",
        type=str,
        default="cobertura",
        required=False,
        choices=["cobertura", "jacoco", "lcov"],
        help="The type of code coverage report to parse. Default is 'cobertura'.",
    )
    parser.add_argument(
        "--exclude-files",
        type=str,
        nargs="+",
        default=[],
        required=False,
        help="A list of files to exclude from mutation testing. Optional.",
    )
    parser.add_argument(
        "--only-mutate-file-paths",
        type=str,
        nargs="+",
        default=[],
        required=False,
        help="A list of specific files to mutate. Optional.",
    )
    parser.add_argument(
        "--diff",
        default=False,
        action="store_true",
        help="Run mutation testing only on modified files in the latest commit.",
    )


def add_gen_line_subparser(subparsers):
    parser = subparsers.add_parser("gen", help="Generate test cases for line coverage.")
    parser.add_argument(
        "--workspace",
        type=Path,
        default=Path.cwd(),
        help="Project directory in which tests and coverage paths are resolved.",
    )
    parser.add_argument(
        "--test-file-path",
        type=Path,
    )
    parser.add_argument(
        "--source-file-path",
        type=Path,
    )
    parser.add_argument(
        "--code-coverage-report-path",
        type=Path,
        required=False,
        help="The path to the code coverage report file. Optional.",
    )
    parser.add_argument(
        "--coverage-type",
        type=str,
        default="cobertura",
        required=False,
        choices=["cobertura", "jacoco", "lcov"],
        help="The type of code coverage report to parse. Default is 'cobertura'.",
    )
    parser.add_argument(
        "--test-command",
        type=str,
        default=None,
        required=True,
        help="The command to run the tests (e.g., 'pytest'). This argument is required.",
    )
    parser.add_argument(
        "--target-line-coverage-rate",
        type=float,
        default=0.9,
        help="The target line coverage rate. Default is 0.9.",
    )
    parser.add_argument(
        "--max-attempts",
        type=int,
        default=3,
        help="The maximum number of attempts to generate a test case. Default is 3.",
    )


def parse_arguments():
    """
    Parses command-line arguments for the Mutahunter CLI.

    Returns:
        argparse.Namespace: Parsed command-line arguments.
    """
    parser = argparse.ArgumentParser(
        description="Mutahunter CLI for performing mutation testing."
    )
    subparsers = parser.add_subparsers(title="commands", dest="command", required=True)
    add_mutation_testing_subparser(subparsers)
    add_gen_line_subparser(subparsers)
    return parser.parse_args()


def normalize_paths(args: argparse.Namespace) -> None:
    """Resolve every user-supplied filesystem path before constructing services."""
    args.workspace = resolve_path(args.workspace)
    for name in ("test_file_path", "source_file_path", "code_coverage_report_path"):
        value = getattr(args, name, None)
        if value is not None:
            setattr(args, name, resolve_path(value, args.workspace))
    for name in ("exclude_files", "only_mutate_file_paths"):
        setattr(args, name, [resolve_path(value, args.workspace) for value in getattr(args, name, [])])


def create_run_mutation_testing_controller(
    args: argparse.Namespace,
) -> MutationTestController:
    model, api_base = configure_backend()
    config = MutationTestControllerConfig(
        model=model,
        api_base=api_base,
        test_command=args.test_command,
        workspace=args.workspace,
        code_coverage_report_path=args.code_coverage_report_path,
        coverage_type=args.coverage_type,
        exclude_files=args.exclude_files,
        only_mutate_file_paths=args.only_mutate_file_paths,
        diff=args.diff,
    )
    coverage_processor = CoverageProcessor(
        code_coverage_report_path=config.code_coverage_report_path,
        coverage_type=config.coverage_type,
        workspace=config.workspace,
    )
    analyzer = Analyzer()
    test_runner = MutantTestRunner(
        test_command=config.test_command, workspace=config.workspace
    )
    prompt = MutationTestingPromptFactory.get_prompt()
    router = LLMRouter(model=config.model, api_base=config.api_base)
    engine = LLMMutationEngine(
        model=config.model, router=router, prompt=prompt, workspace=config.workspace
    )
    db = MutationDatabase()
    mutant_report = MutantReport(db=db)
    file_handler = FileOperationHandler()

    return MutationTestController(
        config=config,
        coverage_processor=coverage_processor,
        analyzer=analyzer,
        test_runner=test_runner,
        router=router,
        engine=engine,
        db=db,
        mutant_report=mutant_report,
        file_handler=file_handler,
        prompt=prompt,
    )


def create_gen_line_controller(args: argparse.Namespace) -> UnittestGenLine:
    model, api_base = configure_backend()
    config = UnittestGeneratorLineConfig(
        model=model,
        api_base=api_base,
        workspace=args.workspace,
        test_file_path=args.test_file_path,
        source_file_path=args.source_file_path,
        test_command=args.test_command,
        code_coverage_report_path=args.code_coverage_report_path,
        coverage_type=args.coverage_type,
        target_line_coverage_rate=args.target_line_coverage_rate,
        max_attempts=args.max_attempts,
    )
    coverage_processor = CoverageProcessor(
        code_coverage_report_path=config.code_coverage_report_path,
        coverage_type=config.coverage_type,
        workspace=config.workspace,
    )
    analyzer = Analyzer()
    router = LLMRouter(model=config.model, api_base=config.api_base)
    prompt = TestGenerationPromptFactory.get_prompt()

    return UnittestGenLine(
        config=config,
        coverage_processor=coverage_processor,
        analyzer=analyzer,
        router=router,
        prompt=prompt,
    )


def run():
    args = parse_arguments()
    normalize_paths(args)
    if args.command == "run":
        controller = create_run_mutation_testing_controller(args)
        controller.run()
    elif args.command == "gen":
        controller = create_gen_line_controller(args)
        controller.run()
    else:
        print("Invalid command.")
        sys.exit(1)


if __name__ == "__main__":
    run()
