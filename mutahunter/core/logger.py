import logging
import warnings
from pathlib import Path

from mutahunter.core.paths import RunPaths

# Suppress specific FutureWarnings from tree_sitter
warnings.filterwarnings("ignore", category=FutureWarning, module="tree_sitter")


def setup_logger(name: str, run_paths: RunPaths | None = None) -> logging.Logger:
    """Create the package logger once, with artifacts rooted in ``RunPaths``."""
    paths = run_paths or RunPaths.default()
    paths.ensure()
    # Create a custom format for your logs
    log_format = "%(asctime)s %(levelname)s: %(message)s"

    # Create a log handler for file output
    file_handler = logging.FileHandler(
        filename=paths.logs / "debug.log",
        mode="w",
        encoding="utf-8",
    )
    stream_handler = logging.StreamHandler()

    # Apply the custom format to the handler
    formatter = logging.Formatter(log_format)
    file_handler.setFormatter(formatter)
    stream_handler.setFormatter(formatter)

    # Create a logger and add the handler
    logger = logging.getLogger(name)
    if logger.handlers:
        return logger
    logger.addHandler(file_handler)
    logger.addHandler(stream_handler)
    logger.setLevel(logging.INFO)

    return logger


logger = setup_logger("mutahunter")
