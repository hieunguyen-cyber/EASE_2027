import subprocess
from pathlib import Path
from typing import Any, Dict, List, Optional

from mutahunter.core.logger import logger


class GitCommandError(Exception):
    """Custom exception for Git command errors."""

    pass


class GitHandler:
    def get_modified_files(covered_files: List[Path], workspace: Path) -> List[Path]:
        try:
            modified_files = GitHandler.run_git_command(
                ["git", "diff", "--name-only", "HEAD"], workspace
            )
            return [
                (workspace / file_path).resolve() for file_path in modified_files
                if (workspace / file_path).resolve() in covered_files
            ]
        except GitCommandError as e:
            logger.error(f"Error identifying modified files: {e}")
            return []

    @staticmethod
    def get_modified_lines(file_path: Path, workspace: Path) -> List[int]:
        try:
            diff_output = GitHandler.run_git_command(
                ["git", "diff", "-U0", "HEAD", str(file_path.relative_to(workspace))], workspace
            )
            return GitHandler._parse_diff_output(diff_output)
        except GitCommandError as e:
            logger.error(f"Error identifying modified lines in {file_path}: {e}")
            return []

    @staticmethod
    def run_git_command(command: List[str], workspace: Path) -> List[str]:
        try:
            output = subprocess.check_output(command, stderr=subprocess.PIPE, cwd=workspace)
            return output.decode("utf-8").splitlines()
        except subprocess.CalledProcessError as e:
            raise GitCommandError(f"Git command failed: {e.stderr.decode('utf-8')}")

    @staticmethod
    def _parse_diff_output(diff_output: List[str]) -> List[int]:
        modified_lines = []
        for line in diff_output:
            if line.startswith("@@"):
                line_numbers = line.split(" ")[2][1:].split(",", 1)
                start_line = int(line_numbers[0])
                line_count = int(line_numbers[1]) if len(line_numbers) > 1 else 1
                modified_lines.extend(range(start_line, start_line + line_count))
        return modified_lines
