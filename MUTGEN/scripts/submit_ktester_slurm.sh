#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
MUTGEN_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
# Run artefacts live beside the code checkout, never inside it.
REPOSITORY_ROOT="$(cd -- "${MUTGEN_ROOT}/.." && pwd)"
JOB_SCRIPT="${MUTGEN_ROOT}/slurm/run_ktester_all.sbatch"
LOG_DIR="${MUTGEN_SLURM_LOG_DIR:-${REPOSITORY_ROOT}/slurm_logs}"

dry_run=0
if [[ "${1:-}" == "--dry-run-submit" ]]; then
  dry_run=1
  shift
fi

mkdir -p "${LOG_DIR}"

export OLLAMA_SERVER_URL="${OLLAMA_SERVER_URL:-http://10.148.182.141:11434}"
export OLLAMA_MODEL="${OLLAMA_MODEL:-qwen2.5-coder:32b}"
export MUTGEN_MODEL="${MUTGEN_MODEL:-ollama/${OLLAMA_MODEL}}"

sbatch_args=(
  --export=ALL
  --chdir="${MUTGEN_ROOT}"
  --output="${LOG_DIR}/%x-%j.out"
  --error="${LOG_DIR}/%x-%j.err"
)

[[ -n "${MUTGEN_SLURM_PARTITION:-}" ]] && sbatch_args+=(--partition="${MUTGEN_SLURM_PARTITION}")
[[ -n "${MUTGEN_SLURM_ACCOUNT:-}" ]] && sbatch_args+=(--account="${MUTGEN_SLURM_ACCOUNT}")
[[ -n "${MUTGEN_SLURM_QOS:-}" ]] && sbatch_args+=(--qos="${MUTGEN_SLURM_QOS}")
[[ -n "${MUTGEN_SLURM_CONSTRAINT:-}" ]] && sbatch_args+=(--constraint="${MUTGEN_SLURM_CONSTRAINT}")
[[ -n "${MUTGEN_SLURM_GRES:-}" ]] && sbatch_args+=(--gres="${MUTGEN_SLURM_GRES}")
[[ -n "${MUTGEN_SLURM_TIME:-}" ]] && sbatch_args+=(--time="${MUTGEN_SLURM_TIME}")
[[ -n "${MUTGEN_SLURM_CPUS:-}" ]] && sbatch_args+=(--cpus-per-task="${MUTGEN_SLURM_CPUS}")
[[ -n "${MUTGEN_SLURM_MEMORY:-}" ]] && sbatch_args+=(--mem="${MUTGEN_SLURM_MEMORY}")

command=(sbatch "${sbatch_args[@]}" "${JOB_SCRIPT}" "$@")
if (( dry_run )); then
  printf '%q ' "${command[@]}"
  printf '\n'
  exit 0
fi

if ! command -v sbatch >/dev/null 2>&1; then
  echo "sbatch is unavailable. Run this script on a Slurm login node." >&2
  exit 127
fi

echo "Submitting MUTGEN with ${OLLAMA_MODEL} at ${OLLAMA_SERVER_URL}"
echo "Logs: ${LOG_DIR}/mutgen-ktester-l33-<jobid>.{out,err}"
"${command[@]}"
