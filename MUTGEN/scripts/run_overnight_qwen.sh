#!/usr/bin/env bash
# Unattended overnight run: prepare -> generate+score all focal methods -> table.
# Designed for tmux. Never aborts the batch because one focal method failed.
set -uo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
MUTGEN_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
# Run artefacts live beside the code checkout, never inside it.
REPOSITORY_ROOT="$(cd -- "${MUTGEN_ROOT}/.." && pwd)"
cd "${MUTGEN_ROOT}"

export PYTHONPATH="${MUTGEN_ROOT}${PYTHONPATH:+:${PYTHONPATH}}"
export OLLAMA_SERVER_URL="${OLLAMA_SERVER_URL:-http://10.148.182.141:11434}"
export OLLAMA_MODEL="${OLLAMA_MODEL:-qwen2.5-coder:32b}"
export MUTGEN_MODEL="${MUTGEN_MODEL:-ollama/${OLLAMA_MODEL}}"
export MUTGEN_RUNS_ROOT="${MUTGEN_RUNS_ROOT:-${REPOSITORY_ROOT}/runs/ktester_qwen25_coder_32b}"
export MUTGEN_ENABLE_MUTATION_FEEDBACK="${MUTGEN_ENABLE_MUTATION_FEEDBACK:-1}"
export MUTGEN_LLM_TIMEOUT_SECONDS="${MUTGEN_LLM_TIMEOUT_SECONDS:-900}"
export MUTGEN_COMMAND_TIMEOUT_SECONDS="${MUTGEN_COMMAND_TIMEOUT_SECONDS:-1800}"

mkdir -p logs
BATCH_LOG="${MUTGEN_RUNS_ROOT}/batch.log"
mkdir -p "${MUTGEN_RUNS_ROOT}"

echo "=========================================================="
echo "MUTGEN overnight run"
echo "  started   : $(date -Is)"
echo "  model     : ${OLLAMA_MODEL}"
echo "  server    : ${OLLAMA_SERVER_URL}"
echo "  runs root : ${MUTGEN_RUNS_ROOT}"
echo "=========================================================="

if ! curl --fail --silent --max-time 30 "${OLLAMA_SERVER_URL}/api/tags" >/dev/null; then
  echo "FATAL: Ollama không phản hồi tại ${OLLAMA_SERVER_URL}" >&2
  exit 1
fi
echo "[ok] Ollama phản hồi."

# Idempotent: completed tasks are skipped, interrupted ones reset and retried.
echo "[1/3] prepare-all"
python3 -u scripts/run_ktester_batch.py prepare-all --runs-root "${MUTGEN_RUNS_ROOT}"

echo "[2/3] run-all (sequential; log: ${BATCH_LOG})"
python3 -u scripts/run_ktester_batch.py run-all \
  --runs-root "${MUTGEN_RUNS_ROOT}" "$@" 2>&1 | tee -a "${BATCH_LOG}"
run_status=${PIPESTATUS[0]}
echo "[run-all] exit=${run_status}"

# Always produce the table, even if some focal methods failed.
echo "[3/3] status-all + results table"
python3 -u scripts/run_ktester_batch.py status-all --runs-root "${MUTGEN_RUNS_ROOT}" || true
python3 -u scripts/report_ktester_table.py --runs-root "${MUTGEN_RUNS_ROOT}" || true

echo "=========================================================="
echo "Xong lúc $(date -Is) (run-all exit=${run_status})"
echo "  Bảng markdown : ${MUTGEN_RUNS_ROOT}/results_table.md"
echo "  CSV           : ${MUTGEN_RUNS_ROOT}/batch_status.csv"
echo "  JSON          : ${MUTGEN_RUNS_ROOT}/batch_status.json"
echo "=========================================================="
