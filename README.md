# 📦 MUTGEN Replication Package

Thank you for your interest in our work. This repository contains the replication package for our paper, including all necessary materials to reproduce the main experimental results. We are still actively improving the quality and organization of the artifact.

---

## 📁 Directory Structure

├── HumanEval-Java_fromscratch/     # Example datasets and run scripts  
├── mutahunter/                     # Core implementation of our technique  
├── scripts/                        # Python utilities for processing  
├── index_humaneval_java.csv        # Subject index for batch runs  
├── requirements.txt                # Python dependencies  
└── README.md

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
hosted Llama 3.3 70B server used for the full repository-level run:

```bash
export OLLAMA_SERVER_URL="http://172.16.0.10:11434"
export OLLAMA_MODEL="llama3.3:70b"
export MUTGEN_ENABLE_MUTATION_FEEDBACK="1"
```

The CLI converts `OLLAMA_MODEL` to the LiteLLM identifier
`ollama/llama3.3:70b`. Set `MUTGEN_MODEL` explicitly when another LiteLLM
provider or identifier is required. Standalone helper scripts use Ollama's
OpenAI-compatible endpoint at `${OLLAMA_SERVER_URL}/v1`.
`MUTGEN_ENABLE_MUTATION_FEEDBACK=1` makes `runeach.sh` extract survived and
uncovered PITest mutants before generating tests; set it to `0` only for a
vanilla-generation control run.

Verify connectivity before starting a batch run:

```bash
curl --fail "${OLLAMA_SERVER_URL}/api/tags"
```

### Fixed LLM runtime

Mutahunter runs only with local Ollama model `llama3.3:70b` at
`http://127.0.0.1:11434`. The model is pulled automatically if it is not installed.

```bash
PYTHONPATH="$PWD" python3 -m mutahunter.main gen \
  --test-command pytest --test-file-path tests/test_app.py \
  --source-file-path app.py
```

### One KTester focal method (repository-level smoke run)

The first prepared task is
`commons-cli / PatternOptionBuilder_getValueType`. Preparation is offline and
copies the repository into `runs/ktester/`, so neither the KTester dataset nor
its original generated test is modified.

```bash
cd /home/hactt13/rs_testing/mutgen_repo/MUTGEN
export PYTHONPATH="$PWD"

# Idempotent; this does not call Ollama.
python3 scripts/run_ktester_focal.py prepare \
  --task-id PatternOptionBuilder_getValueType

# Run when Ollama is available. Requires Java >= 11 and Maven.
export OLLAMA_SERVER_URL="http://172.16.0.10:11434"
export OLLAMA_MODEL="llama3.3:70b"
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
export OLLAMA_SERVER_URL="http://172.16.0.10:11434"
export OLLAMA_MODEL="llama3.3:70b"

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

### Slurm run with hosted Llama 3.3 70B

The Slurm job uses `http://172.16.0.10:11434` and `llama3.3:70b` by default. It
stores results in `runs/ktester_llama33_70b`, separate from earlier Qwen runs,
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
written to `runs/ktester_llama33_70b/batch_status.{json,csv}` when the job exits.

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
