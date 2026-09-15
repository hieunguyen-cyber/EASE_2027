#!/usr/bin/env bash
# Unattended entry point for the KTester+MutGen fusion observation.
#
#   ./run_fuse.sh                      # whole benchmark
#   ./run_fuse.sh --project commons-cli
#   ./run_fuse.sh --max-tasks 3        # smoke run
#
# Everything below can be overridden from the environment before calling.
set -euo pipefail

cd "$(dirname "$0")"

export OLLAMA_SERVER_URL="${OLLAMA_SERVER_URL:-http://10.148.182.141:11434}"
export OLLAMA_MODEL="${OLLAMA_MODEL:-qwen2.5-coder:32b}"
export FUSE_KTESTER_SOURCE="${FUSE_KTESTER_SOURCE:-artifact}"
export FUSE_RUN_NAME="${FUSE_RUN_NAME:-fuse_qwen25_coder_32b}"
export PYTHONPATH="${PWD}:${PYTHONPATH:-}"

echo "=== Cấu hình ==="
python3 config.py
echo

echo "=== Kiểm tra Ollama ==="
if ! curl --silent --fail --max-time 10 "${OLLAMA_SERVER_URL}/api/tags" >/dev/null; then
  echo "Không kết nối được Ollama tại ${OLLAMA_SERVER_URL}" >&2
  exit 1
fi
if ! curl --silent --fail --max-time 10 "${OLLAMA_SERVER_URL}/api/tags" \
     | grep -q "${OLLAMA_MODEL}"; then
  echo "Server không có model '${OLLAMA_MODEL}'" >&2
  exit 1
fi
echo "OK: ${OLLAMA_MODEL} @ ${OLLAMA_SERVER_URL}"
echo

python3 run_batch.py inventory "$@" || true
echo
python3 run_batch.py prepare-all "$@"
echo
python3 run_batch.py run-all "$@"
echo
python3 run_batch.py status-all
python3 report_table.py --only-completed
