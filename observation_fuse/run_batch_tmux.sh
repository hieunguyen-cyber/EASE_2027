#!/usr/bin/env bash
# Batch driver for the tmux session. Resumable: re-running skips completed tasks.
set -uo pipefail
cd "$(dirname "$0")"

export OLLAMA_SERVER_URL="${OLLAMA_SERVER_URL:-http://10.148.182.141:11434}"
export OLLAMA_MODEL="${OLLAMA_MODEL:-qwen2.5-coder:32b}"
export FUSE_KTESTER_SOURCE="${FUSE_KTESTER_SOURCE:-artifact}"
export PYTHONPATH="$PWD"

STAMP="$(date +%Y%m%d_%H%M%S)"
LOG="results/logs/batch_${STAMP}.log"
mkdir -p results/logs
# Link before the run, not after, so the log is tailable while it is running.
ln -sf "batch_${STAMP}.log" results/logs/batch_latest.log

{
  echo "=== bắt đầu $(date -Is) ==="
  python3 config.py
  echo
  python3 run_batch.py run-all --min-free-gib 8 "$@"
  echo
  echo "=== tổng hợp $(date -Is) ==="
  python3 run_batch.py status-all
  python3 report_table.py --only-completed
  echo "=== kết thúc $(date -Is) ==="
} 2>&1 | tee "$LOG"
