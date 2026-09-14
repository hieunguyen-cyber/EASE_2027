# 📦 MUTGEN Replication Package

Thank you for your interest in our work. This repository contains the replication package for our paper, including all necessary materials to reproduce the main experimental results. We are still actively improving the quality and organization of the artifact.

---

## 📁 Directory Structure

This repository contains **code only**. Datasets, toolchains and run artefacts
are kept outside the checkout so the repository stays small and shareable.

```
MUTGEN/                             # <- this git repository (code only)
├── HumanEval-Java_fromscratch/     # Example datasets and run scripts
├── mutahunter/                     # Core implementation of our technique
├── scripts/                        # Python utilities and run drivers
├── slurm/                          # Slurm batch job scripts
├── index_humaneval_java.csv        # Subject index for batch runs
├── requirements.txt                # Python dependencies
└── README.md
```

---

## 🗂️ Working directory layout

The scripts resolve datasets and outputs relative to the **parent** of this
checkout. Clone MUTGEN into a working directory arranged like this:

```
<working-dir>/                      # parent of the MUTGEN checkout
├── MUTGEN/                         # this repository
├── data/
│   ├── projects/puts/              # REQUIRED: KTester subject projects
│   │   └── dataset_info.json       #           + focal-method metadata
│   └── lucene/                     # optional: prebuilt Lucene indexes
├── runs/                           # created by the run scripts
├── logs/                           # created by the run scripts
└── slurm_logs/                     # created by the Slurm submitter
```

`data/projects/puts` is **not** distributed with this repository — obtain it
separately and place it as shown, or override the location:

| Path | Default | Override |
|------|---------|----------|
| Subject projects | `../data/projects/puts` | `MUTGEN_PROJECTS_ROOT` |
| Dataset metadata | `../data/projects/puts/dataset_info.json` | `MUTGEN_DATASET` |
| Run artefacts | `../runs/ktester_qwen25_coder_32b` | `MUTGEN_RUNS_ROOT` |
| Slurm logs | `../slurm_logs` | `MUTGEN_SLURM_LOG_DIR` |

`mutahunter` writes its per-run scratch logs to `logs/_latest` relative to the
working directory it is launched from. If you want those outside the checkout
too, make `MUTGEN/logs` a symlink to the sibling directory:

```bash
ln -s ../logs logs
```

Java toolchains are looked up in `MUTGEN/.tools/jdk-*/bin/java` first, then
`JAVA_HOME`, then the system `PATH`. The `.tools/` directory is git-ignored;
using a system JDK 11 via `JAVA_HOME` works equally well.

---

## 🧩 Component Descriptions

### `HumanEval-Java_fromscratch/`

Includes example datasets and execution scripts used in evaluation:

- `iterrun.sh`: Run iterative testing pipeline across multiple rounds.
- `autorun.sh`: Automatically execute all subjects in the benchmark.
- `runeach.sh`: Run experiments for a single subject.
- `backup/`: Stores original and comment-removed source files.
- `pom.xml`: Maven build configuration for Java testing pipeline.

### `mutahunter/`

Main implementation of our technique:

- Python codebase implementing prompt generation, interaction with LLMs, and result processing.
- Built on top of the Mutahunter framework for generation.

### `scripts/`

Run drivers for the repository-level KTester experiment:

- `run_ktester_focal.py`: Prepare and run one focal method end to end.
- `run_ktester_batch.py`: Drive all focal methods, with per-task status tracking.
- `run_overnight_qwen.sh`: Unattended tmux run over the whole benchmark.
- `submit_ktester_slurm.sh`: Submit the batch as a Slurm job.
- `report_ktester_table.py`: Render the results table from a run root.
- `evaluate_ktester_paper_metrics.py`: Compute the metrics reported in the paper.
- `ollama_config.py`: Shared Ollama endpoint/model resolution.

Utility Python scripts for automated evaluation:

- `copy_from_index.py`: Select test subjects based on index.
- `extractAndremoveComments.py`: Strip comments and summarize Java methods.
- `flip.py`: Flip boolean values in generated test cases (e.g., from EvoSuite).
- `getClassName.py`: Parse Java test files for class names.
- `mergeRegressionRunCsv.py`: Merge results from multiple test iterations.
- `noplan_parsePITestReport.py`: Extract mutation scores from PITest reports.
- `analysisPITestReport.py`: Analyze mutation performance across operators (RQ2-specific).

### `index_humaneval_java.csv`

Contains one Java subject per line. Used for batch job selection and indexing.

### `requirements.txt`

Python dependencies required to run the tool (see next section).

---

## 🛠️ Dependencies

Make sure the following are installed in your environment:

- **Python 3.11**
- **Java 11**
- **EvoSuite 1.2.0**


```bash
python3 -m pip install -r requirements.txt
```

The `mutahunter` package used by this artifact is the local source directory in
this repository, not a package downloaded from PyPI. Run the CLI from the
repository root with:

```bash
PYTHONPATH="$PWD" python3 -m mutahunter.main --help
```

## 🚀 Usage Instructions

### Hosted Ollama configuration

MUTGEN and its standalone preprocessing scripts read the Ollama endpoint and
model from environment variables. The defaults in this checkout point to the
hosted Qwen2.5-Coder 32B server used for the current repository-level run:

```bash
export OLLAMA_SERVER_URL="http://10.148.182.141:11434"
export OLLAMA_MODEL="qwen2.5-coder:32b"
export MUTGEN_ENABLE_MUTATION_FEEDBACK="1"
```

The CLI converts `OLLAMA_MODEL` to the LiteLLM identifier
`ollama/qwen2.5-coder:32b`. Set `MUTGEN_MODEL` explicitly when another LiteLLM
provider or identifier is required. Standalone helper scripts use Ollama's
OpenAI-compatible endpoint at `${OLLAMA_SERVER_URL}/v1`.
`MUTGEN_ENABLE_MUTATION_FEEDBACK=1` makes `runeach.sh` extract survived and
uncovered PITest mutants before generating tests; set it to `0` only for a
vanilla-generation control run.

Verify connectivity before starting a batch run:

```bash
curl --fail "${OLLAMA_SERVER_URL}/api/tags"
```

### One KTester focal method (repository-level smoke run)

The first prepared task is
`commons-cli / PatternOptionBuilder_getValueType`. Preparation is offline and
copies the subject project into the run root, so neither the KTester dataset nor
its original generated test is modified.

```bash
cd /home/hactt13/rs_testing/mutgen_repo/MUTGEN
export PYTHONPATH="$PWD"

# Idempotent; this does not call Ollama.
python3 scripts/run_ktester_focal.py prepare \
  --task-id PatternOptionBuilder_getValueType

# Run when Ollama is available. Requires Java >= 11 and Maven.
export OLLAMA_SERVER_URL="http://10.148.182.141:11434"
export OLLAMA_MODEL="qwen2.5-coder:32b"
python3 scripts/run_ktester_focal.py run \
  --task-id PatternOptionBuilder_getValueType
```

The runner executes PIT before and after one MUTGEN pass. It isolates the
MUTGEN test from all existing repository tests, filters the PIT report to the
exact focal method, validates each generated JUnit 5 test with Maven, and saves
the score plus per-mutant data in
`runs/ktester/PatternOptionBuilder_getValueType/results/summary.json`.

Inspect a prepared, running, failed, or completed experiment with:

```bash
python3 scripts/run_ktester_focal.py status \
  --task-id PatternOptionBuilder_getValueType
```

### All 111 KTester focal methods

The batch runner uses one isolated workspace per focal method and runs
sequentially so a single Ollama-hosted GPU is not oversubscribed. It selects
JDK 11 or 21 per project, resolves Gson tasks to the `gson/` module, uses
KTester's compact `class-code` as the model context, and scopes PIT to the exact
class, overload, and source-line range.

```bash
cd /home/hactt13/rs_testing/mutgen_repo/MUTGEN
export OLLAMA_SERVER_URL="http://10.148.182.141:11434"
export OLLAMA_MODEL="qwen2.5-coder:32b"

# Already prepared in this checkout; safe and idempotent if repeated.
python3 scripts/run_ktester_batch.py inventory
python3 scripts/run_ktester_batch.py prepare-all

# Foreground run.
./scripts/run_all_ktester.sh
```

For an overnight run:

```bash
nohup ./scripts/run_all_ktester.sh \
  > runs/ktester/batch.log 2>&1 &
```

Completed tasks are skipped automatically. If the process was interrupted,
`running` tasks are reset to their clean scaffold and resumed. Explicitly retry
tasks previously marked `failed` with:

```bash
./scripts/run_all_ktester.sh --retry-failed
```

Limit a pilot or select a project without changing the batch manifest:

```bash
./scripts/run_all_ktester.sh --max-tasks 3
./scripts/run_all_ktester.sh --project commons-cli
```

Each task stores the initial PIT report, generated MutGen test, final MutGen
score, KTester reference score, logs, and per-mutant JSON under its run folder.
Before scoring KTester, the runner performs a clean build. Assertion-failing
KTester methods are recorded and disabled, then PIT is run only after the
remaining test class passes; reference compilation failures remain explicit
`FAILED` observations rather than being assigned a mutation score.
The 3 Ruler tasks for which the artifact has no corresponding KTester output are
reported as `REFERENCE_MISSING` and excluded from paired aggregates. Refresh
the resumable aggregate JSON/CSV with:

```bash
python3 scripts/run_ktester_batch.py status-all
```

The prepared workspaces use about 4.2 GB. A complete sequential run on the
single hosted model is expected to take several hours; monitor it with
`tail -f runs/ktester/batch.log`. Command and LLM calls time out after 30 and 15
minutes respectively by default, and these limits can be changed with
`MUTGEN_COMMAND_TIMEOUT_SECONDS` and `MUTGEN_LLM_TIMEOUT_SECONDS`.

### Overnight tmux run and the results table

`scripts/run_overnight_qwen.sh` is the unattended entry point. It checks the
Ollama endpoint, prepares any missing workspace, runs every focal method
sequentially, and always writes the results table at the end -- even when some
focal methods failed.

```bash
cd /home/hactt13/rs_testing/mutgen_repo/MUTGEN
tmux new-session -d -s mutgen_qwen -c "$PWD" \
  "bash scripts/run_overnight_qwen.sh 2>&1 | tee logs/overnight_qwen.log"

tmux attach -t mutgen_qwen          # watch it
tail -f runs/ktester_qwen25_coder_32b/batch.log
```

The batch is resumable: completed focal methods are skipped, `running` ones are
reset to their clean scaffold and retried. It stops itself before free disk
space drops under `--min-free-gib` (default 6, or `MUTGEN_MIN_FREE_GIB`) instead
of dying half-written, and it deletes each task's Maven `target/` tree once
`results/` holds that task's artifacts (`MUTGEN_KEEP_BUILD_OUTPUT=1` keeps it).

Render the table at any time, including mid-run:

```bash
export MUTGEN_RUNS_ROOT="$PWD/runs/ktester_qwen25_coder_32b"
python3 scripts/run_ktester_batch.py status-all --runs-root "$MUTGEN_RUNS_ROOT"
python3 scripts/report_ktester_table.py --only-completed
python3 scripts/report_ktester_table.py --sort-by mutgen_minus_ktester
```

Outputs land in the run root:

| File | Contents |
| --- | --- |
| `results_table.md` | Markdown table, per focal method plus aggregate |
| `batch_status.csv` | Same rows as CSV, one row per focal method |
| `batch_status.json` | Rows plus the aggregate block |

Per-focal-method metrics, all scoped to the focal method only:

| Column | Meaning |
| --- | --- |
| `Tests` / `Pass` / `Fail` | JUnit methods MUTGEN declared, and how many passed |
| `Mutants` | PIT mutants inside the focal method |
| `Base MS` | Mutation score of the empty scaffold (baseline) |
| `Killed` / `Surv` / `NoCov` | MUTGEN killed, survived, not covered |
| `MutGen MS` | MUTGEN mutation score |
| `Gain` | MUTGEN minus baseline, in percentage points |
| `K/test` | Mutants killed per generated test method |
| `KT tests` / `KT MS` | KTester reference test count and mutation score |
| `Δ MS` | MUTGEN minus KTester |
| `Winner` | `mutgen`, `ktester`, or `tie` |

A focal method whose KTester artifact does not compile is reported as
`REFERENCE_UNCOMPILABLE` and excluded from the paired aggregates rather than
being given a score. The 3 Ruler tasks with no KTester output stay
`REFERENCE_MISSING`.

### Slurm run with hosted Qwen2.5-Coder 32B

The Slurm job uses `http://10.148.182.141:11434` and `qwen2.5-coder:32b` by
default. It stores results in `runs/ktester_qwen25_coder_32b`,
and runs focal methods sequentially so the hosted Ollama model receives only
one generation request at a time.

From a Slurm login node:

```bash
cd /path/to/MUTGEN
./scripts/submit_ktester_slurm.sh
```

The submit helper creates `slurm_logs/` and prints the job ID. Configure
cluster-specific scheduling fields without editing the job file:

```bash
MUTGEN_SLURM_PARTITION=compute \
MUTGEN_SLURM_ACCOUNT=my-account \
MUTGEN_SLURM_TIME=48:00:00 \
MUTGEN_SLURM_CPUS=8 \
MUTGEN_SLURM_MEMORY=32G \
MUTGEN_MODULES="python/3.10 java/21" \
MUTGEN_VENV=/path/to/venv \
./scripts/submit_ktester_slurm.sh
```

The client job does not request a GPU by default because inference is already
performed by the remote A100 Ollama server. If cluster policy requires an A100
allocation for the client job, add for example:

```bash
MUTGEN_SLURM_GRES=gpu:a100:1 ./scripts/submit_ktester_slurm.sh
```

Useful variants:

```bash
# Check the sbatch command without submitting.
./scripts/submit_ktester_slurm.sh --dry-run-submit

# Five-task smoke run.
./scripts/submit_ktester_slurm.sh --max-tasks 5

# Retry tasks previously recorded as failed.
./scripts/submit_ktester_slurm.sh --retry-failed

# Generate and score only MutGen, omitting the local KTester reference PIT run.
MUTGEN_SCORE_KTESTER=0 ./scripts/submit_ktester_slurm.sh
```

The job validates Python dependencies, Java 21 availability, and the exact
Ollama model tag before starting builds. It keeps a lock in the run directory
to prevent concurrent jobs from corrupting the same task workspace. Completed
tasks are checkpointed and skipped on resubmission; an interrupted `running`
task is reset to its scaffold and retried automatically. Aggregate progress is
written to `runs/ktester_qwen25_coder_32b/batch_status.{json,csv}` when the job exits.

After installing all required dependencies, follow the steps below to reproduce the experimental results:

1. Navigate to the benchmark directory:

```bash
cd HumanEval-Java_fromscratch
```

2. Use the following Maven command to generate and export EvoSuite test cases. These tests can be saved and reused for further analysis:
```bash
mvn evosuite:generate evosuite:export
```

3. Before running, make sure to specify the desired number of iterations in the iterrun script:
```bash 
./iterrun 
```
This script performs multi-round evaluation and will generate one folder per iteration (e.g., iter1_log, iter2_log, etc.).

4. Each iteration produces a log directory (itern_log) containing raw mutation testing results. For each log, extract the results using:
```bash
cd iter1_log && ./getResFromPITlog.sh > iter1.csv
```
Repeat this step for all iterations, then merge the result files using the provided script (used in RQ1 analysis):
```bash
python mergeRegressionRunCsv.py ../index_humaneval_java.csv iter1_log/iter1.csv ... itern_log/itern.csv
```
