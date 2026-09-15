"""Centralized, server-safe paths for MUTGEN.

All default locations are anchored at the repository root, never at the
process working directory.  Deployments can redirect generated artifacts with
``MUTGEN_OUTPUT_ROOT`` without changing source code.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
DATA_ROOT = REPOSITORY_ROOT / "data" / "projects" / "puts"


def resolve_path(path: Path | str, base: Path | None = None) -> Path:
    """Return an absolute normalized path, resolving relative paths from base."""
    candidate = Path(path).expanduser()
    if not candidate.is_absolute():
        candidate = (base or Path.cwd()) / candidate
    return candidate.resolve()


def output_root() -> Path:
    configured = os.environ.get("MUTGEN_OUTPUT_ROOT")
    return resolve_path(configured, REPOSITORY_ROOT) if configured else REPOSITORY_ROOT / "runs" / "mutahunter"


@dataclass(frozen=True)
class RunPaths:
    """Filesystem layout for mutable artifacts from a MUTGEN invocation."""

    root: Path

    @classmethod
    def default(cls) -> "RunPaths":
        return cls(output_root())

    @property
    def latest(self) -> Path:
        return self.root / "latest"

    @property
    def logs(self) -> Path:
        return self.latest / "logs"

    @property
    def llm(self) -> Path:
        return self.logs / "llm"

    @property
    def mutants(self) -> Path:
        return self.logs / "mutants"

    @property
    def unittest(self) -> Path:
        return self.logs / "unittest"

    @property
    def html(self) -> Path:
        return self.latest / "html"

    @property
    def database(self) -> Path:
        return self.latest / "mutahunter.sqlite3"

    def ensure(self) -> None:
        for directory in (self.root, self.latest, self.logs, self.llm, self.mutants, self.unittest, self.html):
            directory.mkdir(parents=True, exist_ok=True)
