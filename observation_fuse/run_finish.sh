#!/usr/bin/env bash
# Wait for the main batch to end, retry whatever failed, then write the report.
# Runs detached in its own tmux session so it survives the shell that started it.
set -uo pipefail
cd "$(dirname "$0")"

export OLLAMA_SERVER_URL="${OLLAMA_SERVER_URL:-http://10.148.182.141:11434}"
export OLLAMA_MODEL="${OLLAMA_MODEL:-qwen2.5-coder:32b}"
export PYTHONPATH="$PWD"
LOG="results/logs/finish_$(date +%Y%m%d_%H%M%S).log"
mkdir -p results/logs
ln -sf "$(basename "$LOG")" results/logs/finish_latest.log

{
  echo "=== chờ batch chính kết thúc: $(date -Is) ==="
  # "=fuse" forces an exact match: tmux resolves a bare "fuse" by prefix, so it
  # would match this very session (fuse_finish) and wait on itself forever.
  while tmux has-session -t '=fuse' 2>/dev/null; do sleep 60; done
  echo "=== batch chính xong: $(date -Is) ==="
  python3 run_batch.py inventory | tail -1

  # Retry pass. Tasks whose seed genuinely does not compile will fail again and
  # that is the correct outcome -- they are excluded from the paired aggregate,
  # not scored as zero.
  echo
  echo "=== retry các task failed: $(date -Is) ==="
  python3 run_batch.py run-all --retry-failed --min-free-gib 8

  echo
  echo "=== tổng hợp cuối: $(date -Is) ==="
  python3 run_batch.py inventory | tail -1
  python3 run_batch.py status-all
  python3 report_table.py --only-completed
  echo "=== xong: $(date -Is) ==="
} 2>&1 | tee "$LOG"
