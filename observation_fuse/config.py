"""Paths, model endpoints and run layout for the KTester+MutGen fusion observation.

Everything is overridable through environment variables so the same checkout can
drive a smoke run, a single project, or the full 111-focal-method benchmark.
"""

from __future__ import annotations

import os
from pathlib import Path


# --------------------------------------------------------------------------
# Repository layout
# --------------------------------------------------------------------------
OBS_ROOT = Path(__file__).resolve().parent
WORKDIR_ROOT = OBS_ROOT.parent                      # .../rs_testing/mutgen_repo
MUTGEN_ROOT = Path(os.getenv("FUSE_MUTGEN_ROOT", WORKDIR_ROOT / "MUTGEN"))
KTESTER_ROOT = Path(os.getenv("FUSE_KTESTER_ROOT", WORKDIR_ROOT / "KTester"))

DATA_ROOT = Path(os.getenv("FUSE_DATA_ROOT", WORKDIR_ROOT / "data"))
PROJECTS_ROOT = Path(os.getenv("FUSE_PROJECTS_ROOT", DATA_ROOT / "projects/puts"))
DATASET = Path(os.getenv("FUSE_DATASET", PROJECTS_ROOT / "dataset_info.json"))

# KTester knowledge index. `json/` and `codegraph/` ship with the KTester repo;
# `lucene/` comes from the separately downloaded dataset archive.
KTESTER_INDEX_JSON = KTESTER_ROOT / "data/project_index/json"
KTESTER_INDEX_CODEGRAPH = KTESTER_ROOT / "data/project_index/codegraph"
KTESTER_INDEX_LUCENE = Path(os.getenv("FUSE_LUCENE_ROOT", DATA_ROOT / "lucene"))

# --------------------------------------------------------------------------
# Results
# --------------------------------------------------------------------------
RESULTS_ROOT = Path(os.getenv("FUSE_RESULTS_ROOT", OBS_ROOT / "results"))
RUN_NAME = os.getenv("FUSE_RUN_NAME", "fuse_qwen25_coder_32b")
RUN_ROOT = Path(os.getenv("FUSE_RUN_ROOT", RESULTS_ROOT / RUN_NAME))
TASKS_ROOT = RUN_ROOT / "tasks"

# Optional: the existing from-scratch MutGen run, imported for context only.
MUTGEN_SCRATCH_RUN = Path(
    os.getenv("FUSE_MUTGEN_SCRATCH_RUN", WORKDIR_ROOT / "runs/ktester_qwen25_coder_32b")
)

# --------------------------------------------------------------------------
# Models
# --------------------------------------------------------------------------
# Stage B (MutGen) talks to Ollama through LiteLLM.
OLLAMA_SERVER_URL = os.getenv("OLLAMA_SERVER_URL", "http://10.148.182.141:11434").rstrip("/")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5-coder:32b")
MUTGEN_MODEL = os.getenv("MUTGEN_MODEL", f"ollama/{OLLAMA_MODEL}")

# Stage A (KTester reproduction) talks to any OpenAI-compatible endpoint.
# Ollama exposes one at /v1 and ignores the key, so the default needs no secret.
API_KEY_PLACEHOLDER = "REPLACE_ME_SET_KTESTER_LLM_API_KEY"
KTESTER_LLM_BASE_URL = os.getenv("KTESTER_LLM_BASE_URL", f"{OLLAMA_SERVER_URL}/v1")
KTESTER_LLM_MODEL = os.getenv("KTESTER_LLM_MODEL", OLLAMA_MODEL)
KTESTER_LLM_API_KEY = os.getenv("KTESTER_LLM_API_KEY", "ollama")
KTESTER_LLM_TEMPERATURE = float(os.getenv("KTESTER_LLM_TEMPERATURE", "0.5"))

# --------------------------------------------------------------------------
# Stage behaviour
# --------------------------------------------------------------------------
# "artifact": seed from the released KTester test class (no Stage A LLM calls).
# "generate": re-run the KTester pipeline with KTESTER_LLM_MODEL.
KTESTER_SOURCE = os.getenv("FUSE_KTESTER_SOURCE", "artifact")

# How many undetected mutants to show the model in one feedback block.
MAX_FEEDBACK_MUTANTS = int(os.getenv("FUSE_MAX_FEEDBACK_MUTANTS", "40"))
# Mutation-feedback generation rounds. Each round re-runs PIT and re-prompts.
FUSE_ROUNDS = int(os.getenv("FUSE_ROUNDS", "1"))

PITEST_VERSION = "1.18.0"


def require_ktester_api_key() -> None:
    """Fail loudly before spending time when Stage A has no usable credential."""
    is_ollama = KTESTER_LLM_BASE_URL.startswith(OLLAMA_SERVER_URL)
    if is_ollama:
        return
    if not KTESTER_LLM_API_KEY or KTESTER_LLM_API_KEY in {"ollama", API_KEY_PLACEHOLDER}:
        raise SystemExit(
            "Stage A dùng endpoint không phải Ollama nên cần API key thật.\n"
            f"  export KTESTER_LLM_BASE_URL={KTESTER_LLM_BASE_URL}\n"
            "  export KTESTER_LLM_API_KEY=<key>\n"
            f"  export KTESTER_LLM_MODEL={KTESTER_LLM_MODEL}"
        )


def describe() -> str:
    return "\n".join(
        [
            f"workdir          : {WORKDIR_ROOT}",
            f"MUTGEN           : {MUTGEN_ROOT}",
            f"KTester          : {KTESTER_ROOT}",
            f"dataset          : {DATASET}",
            f"projects         : {PROJECTS_ROOT}",
            f"run root         : {RUN_ROOT}",
            f"stage A source   : {KTESTER_SOURCE}",
            f"stage A model    : {KTESTER_LLM_MODEL} @ {KTESTER_LLM_BASE_URL}",
            f"stage B model    : {MUTGEN_MODEL} @ {OLLAMA_SERVER_URL}",
            f"feedback mutants : {MAX_FEEDBACK_MUTANTS}",
            f"fuse rounds      : {FUSE_ROUNDS}",
        ]
    )


if __name__ == "__main__":
    print(describe())
