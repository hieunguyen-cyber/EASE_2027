#!/usr/bin/env bash
#SBATCH --job-name=mutgen-ktester-l33
#SBATCH --time=48:00:00
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=8
#SBATCH --mem=32G
#SBATCH --signal=B:TERM@120
#SBATCH --output=slurm-%x-%j.out
#SBATCH --error=slurm-%x-%j.err

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
MUTGEN_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
cd "${MUTGEN_ROOT}"

# Cluster module names are site-specific. Example:
#   MUTGEN_MODULES="python/3.10 java/21" ./scripts/submit_ktester_slurm.sh
if [[ -n "${MUTGEN_MODULES:-}" ]]; then
  if ! type module >/dev/null 2>&1; then
    for module_init in /etc/profile.d/modules.sh /usr/share/Modules/init/bash; do
      if [[ -r "${module_init}" ]]; then
        # shellcheck disable=SC1090
        source "${module_init}"
        break
      fi
    done
  fi
  if ! type module >/dev/null 2>&1; then
    echo "MUTGEN_MODULES is set, but the environment-modules command is unavailable." >&2
    exit 2
  fi
  read -r -a mutgen_modules <<< "${MUTGEN_MODULES}"
  module load "${mutgen_modules[@]}"
fi

if [[ -n "${MUTGEN_VENV:-}" ]]; then
  if [[ ! -f "${MUTGEN_VENV}/bin/activate" ]]; then
    echo "MUTGEN_VENV does not contain bin/activate: ${MUTGEN_VENV}" >&2
    exit 2
  fi
  # shellcheck disable=SC1090
  source "${MUTGEN_VENV}/bin/activate"
elif [[ -f "${MUTGEN_ROOT}/.venv/bin/activate" ]]; then
  # shellcheck disable=SC1091
  source "${MUTGEN_ROOT}/.venv/bin/activate"
fi

PYTHON_BIN="${MUTGEN_PYTHON:-python3}"
if ! command -v "${PYTHON_BIN}" >/dev/null 2>&1; then
  echo "Python executable not found: ${PYTHON_BIN}" >&2
  exit 2
fi

export PYTHONPATH="${MUTGEN_ROOT}${PYTHONPATH:+:${PYTHONPATH}}"
export OLLAMA_SERVER_URL="${OLLAMA_SERVER_URL:-http://172.16.0.10:11434}"
export OLLAMA_MODEL="${OLLAMA_MODEL:-llama3.3:70b}"
export MUTGEN_MODEL="${MUTGEN_MODEL:-ollama/${OLLAMA_MODEL}}"
export MUTGEN_LLM_TIMEOUT_SECONDS="${MUTGEN_LLM_TIMEOUT_SECONDS:-1800}"
export MUTGEN_COMMAND_TIMEOUT_SECONDS="${MUTGEN_COMMAND_TIMEOUT_SECONDS:-3600}"
export MAVEN_OPTS="${MAVEN_OPTS:--Xms512m -Xmx8g -XX:+UseG1GC}"

# A separate root prevents completed Qwen runs from being mistaken for Llama runs.
RUNS_ROOT="${MUTGEN_RUNS_ROOT:-${MUTGEN_ROOT}/runs/ktester_qwen25_coder_32b}"
DATASET="${MUTGEN_DATASET:-${MUTGEN_ROOT}/../data/projects/puts/dataset_info.json}"
PROJECTS_ROOT="${MUTGEN_PROJECTS_ROOT:-${MUTGEN_ROOT}/../data/projects/puts}"

if [[ ! -f "${DATASET}" ]]; then
  echo "KTester dataset metadata not found: ${DATASET}" >&2
  echo "Upload the sibling data/projects/puts directory or set MUTGEN_DATASET." >&2
  exit 2
fi
if [[ ! -d "${PROJECTS_ROOT}" ]]; then
  echo "KTester projects root not found: ${PROJECTS_ROOT}" >&2
  exit 2
fi
mkdir -p "${RUNS_ROOT}"

if command -v flock >/dev/null 2>&1; then
  exec 9>"${RUNS_ROOT}/.slurm.lock"
  if ! flock -n 9; then
    echo "Another job is already using ${RUNS_ROOT}; refusing concurrent writes." >&2
    exit 3
  fi
fi

echo "MUTGEN root:       ${MUTGEN_ROOT}"
echo "Slurm job:         ${SLURM_JOB_ID:-not-running-under-slurm}"
echo "Node:              ${SLURMD_NODENAME:-$(hostname)}"
echo "Runs root:         ${RUNS_ROOT}"
echo "Ollama endpoint:   ${OLLAMA_SERVER_URL}"
echo "Ollama model:      ${OLLAMA_MODEL}"
echo "LiteLLM model:     ${MUTGEN_MODEL}"
echo "Python:            $(command -v "${PYTHON_BIN}")"

"${PYTHON_BIN}" - <<'PY'
required = ["litellm", "pydantic", "yaml", "tree_sitter", "tree_sitter_languages"]
missing = []
for package in required:
    try:
        __import__(package)
    except ImportError:
        missing.append(package)
if missing:
    raise SystemExit(
        "Missing Python packages: " + ", ".join(missing)
        + ". Install requirements.txt in MUTGEN_VENV before submitting."
    )
PY

# The full dataset includes commons-codec, whose snapshot needs Java 21.
"${PYTHON_BIN}" - <<'PY'
from scripts.run_ktester_focal import configure_java
path, version, major = configure_java(21)
print(f"Java 21 preflight: {path} (major={major})")
PY

# Fail before copying/building projects if the endpoint or exact model tag is wrong.
"${PYTHON_BIN}" - <<'PY'
import os
from scripts.run_ktester_focal import check_ollama
url = os.environ["OLLAMA_SERVER_URL"].rstrip("/")
model = os.environ["OLLAMA_MODEL"]
check_ollama(url, model)
print(f"Ollama preflight passed: {url} / {model}")
PY

"${PYTHON_BIN}" scripts/run_ktester_batch.py inventory \
  --dataset "${DATASET}" \
  --projects-root "${PROJECTS_ROOT}" \
  --runs-root "${RUNS_ROOT}"

batch_args=(
  --dataset "${DATASET}"
  --projects-root "${PROJECTS_ROOT}"
  --runs-root "${RUNS_ROOT}"
  --max-feedback-mutants "${MUTGEN_MAX_FEEDBACK_MUTANTS:-80}"
)

case "${MUTGEN_SCORE_KTESTER:-1}" in
  0|false|FALSE|no|NO) batch_args+=(--skip-ktester-score) ;;
  1|true|TRUE|yes|YES) ;;
  *)
    echo "MUTGEN_SCORE_KTESTER must be 0/1, false/true, or no/yes." >&2
    exit 2
    ;;
esac

child_pid=""
on_signal() {
  echo "Received a termination signal; stopping the active task. Resubmit to resume." >&2
  if [[ -n "${child_pid}" ]] && kill -0 "${child_pid}" 2>/dev/null; then
    kill -TERM "${child_pid}" 2>/dev/null || true
    wait "${child_pid}" || true
  fi
  exit 143
}
on_exit() {
  local rc=$?
  trap - EXIT
  "${PYTHON_BIN}" scripts/run_ktester_batch.py status-all \
    --dataset "${DATASET}" \
    --projects-root "${PROJECTS_ROOT}" \
    --runs-root "${RUNS_ROOT}" || true
  echo "Job finished with exit code ${rc}."
  exit "${rc}"
}
trap on_signal TERM INT
trap on_exit EXIT

"${MUTGEN_ROOT}/scripts/run_all_ktester.sh" "${batch_args[@]}" "$@" &
child_pid=$!
wait "${child_pid}"
