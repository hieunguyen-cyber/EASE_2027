from dataclasses import dataclass
from pathlib import Path
from typing import Optional


@dataclass
class MutationTestControllerConfig:
    model: str
    api_base: str
    test_command: str
    workspace: Path
    code_coverage_report_path: Optional[Path]
    coverage_type: str
    exclude_files: list[Path]
    only_mutate_file_paths: list[Path]
    diff: bool


@dataclass
class UnittestGeneratorLineConfig:
    model: str
    api_base: str
    workspace: Path
    test_file_path: Path
    source_file_path: Path
    test_command: str
    code_coverage_report_path: Optional[Path]
    coverage_type: str
    target_line_coverage_rate: float
    max_attempts: int
    source_description: str = ""
    mutation_feedback: str = ""
    test_framework: str = "JUnit 5"
    source_code_override: str = ""
