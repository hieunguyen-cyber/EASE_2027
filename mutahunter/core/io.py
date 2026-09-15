from pathlib import Path
from typing import Any, Dict, List, Optional
from uuid import uuid4

from grep_ast import filename_to_lang
from tree_sitter_languages import get_language, get_parser

from mutahunter.core.paths import RunPaths

TEST_FILE_PATTERNS = [
    "test_",
    "_test",
    ".test",
    ".spec",
    ".tests",
    ".Test",
    "tests/",
    "test/",
]


class FileOperationHandler:
    @staticmethod
    def read_file(file_path: Path) -> str:
        return file_path.read_text(encoding="utf-8")

    @staticmethod
    def write_file(file_path: Path, content: str) -> None:
        file_path.parent.mkdir(parents=True, exist_ok=True)
        file_path.write_text(content, encoding="utf-8")

    @staticmethod
    def get_mutant_path(source_file_path: Path, mutant_id: str) -> Path:
        mutant_file_name = f"{mutant_id}_{source_file_path.name}"
        paths = RunPaths.default()
        paths.ensure()
        return paths.mutants / mutant_file_name

    @staticmethod
    def prepare_mutant_file(
        mutant_data: Dict[str, Any], source_file_path: Path
    ) -> Optional[Path]:
        mutant_id = str(uuid4())[:8]
        mutant_path = FileOperationHandler.get_mutant_path(source_file_path, mutant_id)
        source_code = FileOperationHandler.read_file(source_file_path)
        applied_mutant = FileOperationHandler.apply_mutation(source_code, mutant_data)
        if not FileOperationHandler.check_syntax(source_file_path, applied_mutant):
            raise SyntaxError("Mutant syntax is incorrect.")
        FileOperationHandler.write_file(mutant_path, applied_mutant)
        return mutant_path

    @staticmethod
    def should_skip_file(
        filename: Path, exclude_files: List[Path], only_mutate_file_paths: List[Path]
    ) -> bool:
        if only_mutate_file_paths:
            for file_path in only_mutate_file_paths:
                if not file_path.exists():
                    raise FileNotFoundError(f"File {file_path} does not exist.")
            return all(file_path != filename for file_path in only_mutate_file_paths)
        if filename in exclude_files:
            return True

    @staticmethod
    def check_syntax(source_file_path: Path, source_code: str) -> bool:
        """
        Checks the syntax of the provided source code.

        Args:
            source_code (str): The source code to check.

        Returns:
            bool: True if the syntax is correct, False otherwise.
        """
        lang = filename_to_lang(str(source_file_path))
        parser = get_parser(lang)
        tree = parser.parse(bytes(source_code, "utf8"))
        return not tree.root_node.has_error

    @staticmethod
    def apply_mutation(source_code: str, mutant_data: Dict[str, Any]) -> str:
        src_code_lines = source_code.splitlines(keepends=True)
        mutated_line = mutant_data["mutated_code"].strip()
        line_number = mutant_data["line_number"]

        indentation = len(src_code_lines[line_number - 1]) - len(
            src_code_lines[line_number - 1].lstrip()
        )
        src_code_lines[line_number - 1] = " " * indentation + mutated_line + "\n"

        return "".join(src_code_lines)
