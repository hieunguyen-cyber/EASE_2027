import shutil
from pathlib import Path

from mutahunter.core.logger import logger


class FileUtils:
    @staticmethod
    def read_file(path: Path) -> str:
        try:
            return path.read_text(encoding="utf-8", errors="ignore")
        except FileNotFoundError:
            logger.info(f"File not found: {path}")
        except Exception as e:
            logger.info(f"Error reading file {path}: {e}")
            raise

    @staticmethod
    def number_lines(code: str) -> str:
        return "\n".join(f"{i + 1} {line}" for i, line in enumerate(code.splitlines()))

    @staticmethod
    def backup_code(file_path: Path) -> None:
        backup_path = file_path.with_suffix(file_path.suffix + ".bak")
        try:
            shutil.copyfile(file_path, backup_path)
        except Exception as e:
            logger.info(f"Failed to create backup file for {file_path}: {e}")
            raise

    @staticmethod
    def revert(file_path: Path) -> None:
        backup_path = file_path.with_suffix(file_path.suffix + ".bak")
        try:
            if backup_path.exists():
                shutil.copyfile(backup_path, file_path)
                backup_path.unlink()
            else:
                logger.info(f"No backup file found for {file_path}")
                raise FileNotFoundError(f"No backup file found for {file_path}")
        except Exception as e:
            logger.info(f"Failed to revert file {file_path}: {e}")
            raise
