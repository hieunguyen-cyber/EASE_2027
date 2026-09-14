#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
MUTGEN_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

cd "${MUTGEN_ROOT}"
export PYTHONPATH="${MUTGEN_ROOT}${PYTHONPATH:+:${PYTHONPATH}}"
export OLLAMA_SERVER_URL="${OLLAMA_SERVER_URL:-http://10.148.182.141:11434}"
export OLLAMA_MODEL="${OLLAMA_MODEL:-qwen2.5-coder:32b}"
export MUTGEN_MODEL="${MUTGEN_MODEL:-ollama/${OLLAMA_MODEL}}"
export MUTGEN_LLM_TIMEOUT_SECONDS="${MUTGEN_LLM_TIMEOUT_SECONDS:-900}"
export MUTGEN_COMMAND_TIMEOUT_SECONDS="${MUTGEN_COMMAND_TIMEOUT_SECONDS:-1800}"

exec python3 -u scripts/run_ktester_batch.py run-all "$@"
