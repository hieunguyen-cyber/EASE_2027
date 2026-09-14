#!/bin/bash

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
MUTGEN_ROOT=$(cd -- "${SCRIPT_DIR}/.." && pwd)
OLLAMA_SERVER_URL="${OLLAMA_SERVER_URL:-http://172.16.0.10:11434}"
OLLAMA_MODEL="${OLLAMA_MODEL:-llama3.3:70b}"
MUTGEN_MODEL="${MUTGEN_MODEL:-ollama/${OLLAMA_MODEL}}"
MUTGEN_PYTHON="${MUTGEN_PYTHON:-python3}"
MUTGEN_ENABLE_MUTATION_FEEDBACK="${MUTGEN_ENABLE_MUTATION_FEEDBACK:-1}"

# Define the directory containing Java source files
JAVA_SRC_DIR="src/main/java/original/"
JAVA_SRCTEST_DIR="src/test/java/original/"
PITEST_REPORT_DIR="oriReport/pit-reports/original/"
TEST_USER_PROMPT_FILE="${MUTGEN_ROOT}/mutahunter/core/templates/test_generation/test_generator_user.txt"
ANALYZER_USER_PROMPT_FILE="${MUTGEN_ROOT}/mutahunter/core/templates/test_generation/analyzer_user.txt"

GETNAME_SCRIPT="../scripts/getClassName.py"
COMMENT_PROCESSING_SCRIPT="../scripts/extractAndremoveComments.py"
MUTATIONREPORT_PROCESSING_SCRIPT="../scripts/parsePITestReport.py"
NOPLAN_MUTATIONREPORT_PROCESSING_SCRIPT="../scripts/noplan_parsePITestReport.py"
BACKUP_JAVA_SRC_DIR="backup/${JAVA_SRC_DIR}"
BACKUP_JAVA_SRCTEST_DIR="iterRES/"
RES_DIR="res/"

java_file=$1
filename=$(basename "$java_file")
prefix=$(basename "$java_file" .java)
report_file="$prefix.java.html"
if [ -e "genlog_$prefix.txt" ]; then
    echo "genlog_$prefix.txt exists, skip"
    exit
fi    
# get class name from java src file
class_name=$("${MUTGEN_PYTHON}" "$GETNAME_SCRIPT" "$java_file" 2>/dev/null)
if [ -n "$class_name" ]; then # TODO: catch exception
    echo "<-----$class_name, $prefix----->"
else
    echo "No class found in: $java_file"
    exit
fi

if [ ! -f "${BACKUP_JAVA_SRCTEST_DIR}${class_name}_ESTest.java" ]; then
    echo "Test case doesn't exists in given searching foler. Exit..."
    exit
else
    cp "${BACKUP_JAVA_SRCTEST_DIR}${class_name}_ESTest"* "${JAVA_SRCTEST_DIR}"
fi

# get comment on src code 1) extract comment and delete comment in src; 2) add comment to test generator user prompt.
description_content=$("${MUTGEN_PYTHON}" "$COMMENT_PROCESSING_SCRIPT" "$java_file" 2>&1)
if [ $? -eq 0 ]; then
    echo "Comment processing successful"
else 
    echo "Comment processing failed, as $description_content"
    exit
fi
compBefore="mvn package"
error_message=$( $compBefore > compileBeforeGen_err${prefix}.txt 2>&1 )
if [ $? -eq 0 ]; then
    echo "mvn package before generation successful"
    rm compileBeforeGen_err${prefix}.txt
else
    echo "mvn package error. Error message written to compileBeforeGen_err${prefix}.txt"
    exit
fi
genUpdReport="mvn org.pitest:pitest-maven:mutationCoverage -DtargetClasses=original.${class_name} -DtargetTests=original.${class_name}_ESTest"
error_message=$( $genUpdReport > pitexec_err${prefix}.txt 2>&1 )
if [ $? -eq 0 ]; then
    echo "Pitest execution successful"
    mv pitexec_err${prefix}.txt pitBefore${prefix}.txt
else
    echo "Pitest execution failed. Error message written to pitexec_err${prefix}.txt"
    exit
fi
cphtml=$(cp target/pit-reports/original/${report_file} $PITEST_REPORT_DIR)
if [ $? -eq 0 ]; then
    echo "Copy to oriReport successful"
else 
    echo "Copy to oriReport failed"
    exit
fi

skip_generation=false
if [ "$MUTGEN_ENABLE_MUTATION_FEEDBACK" = "1" ]; then
    report="$PITEST_REPORT_DIR$report_file"
    # Extract survived/uncovered mutants and inject them into the local test-generation prompt.
    if [ -e "$report" ]; then
        echo "Mutation report exists"
    else
        echo "Mutation report does not exist: $report"
        exit 1
    fi
    mutation_report=$("${MUTGEN_PYTHON}" "$NOPLAN_MUTATIONREPORT_PROCESSING_SCRIPT" "$report" "$java_file" 2>&1)
    if [ $? -eq 0 ]; then
        echo "Mutation report processing successful"
        if echo "$mutation_report" | grep -q "No available live mutants."; then
            echo "No survived or uncovered mutants; skipping LLM generation"
            skip_generation=true
        fi
    else
        echo "Mutation report processing failed:"
        echo "$mutation_report"
        exit 1
    fi
else
    echo "Mutation feedback disabled"
fi
cp "${TEST_USER_PROMPT_FILE}" "./test_generator_user_${class_name}.txt"

# generate additional test cases
# Define the command
command="PYTHONPATH=\"${MUTGEN_ROOT}${PYTHONPATH:+:${PYTHONPATH}}\" ${MUTGEN_PYTHON} -m mutahunter.main gen --test-command \"mvn package\" \
  --code-coverage-report-path \"target/site/jacoco/jacoco.xml\" \
  --source-file-path \"src/main/java/original/${prefix}.java\" \
  --test-file-path \"src/test/java/original/${class_name}_ESTest.java\" \
  --coverage-type jacoco \
  --model \"${MUTGEN_MODEL}\" \
  --api-base \"${OLLAMA_SERVER_URL}\" \
  --target-line-coverage-rate 1.0 \
  --max-attempts 1 > genlog_${prefix}.txt 2>&1"
if [ "$skip_generation" = false ]; then
    eval $command
    exit_code=$?
elif [ "$skip_generation" = true ]; then
    exit_code=0
fi
if [ $exit_code -eq 0 ]; then
    echo "Generation stage finished. Proceeding to next step......"
else
    echo "gen Command failed with exit_code $exit_code"
    exit
fi

# compile test with additional test cases
compnew="javac -cp target/classes:../lib/evosuite-1.2.0.jar -d target/test-classes src/test/java/original/${class_name}*.java"
error_message=$( $compnew > compnew_err${prefix}.txt 2>&1 )
if [ $? -eq 0 ]; then
    echo "Compile new test successful"
    rm compnew_err${prefix}.txt
else
    echo "Compile failed. Error message written to compnew_err${prefix}.txt"
    exit
fi
cp src/test/java/original/${class_name}*.java $RES_DIR

mvn clean > /dev/null
compAfter="mvn package"
error_message=$( $compAfter > compileAfterGen_err${prefix}.txt 2>&1 )
if [ $? -eq 0 ]; then
    echo "mvn package after generation successful"
    rm compileAfterGen_err${prefix}.txt
else
    echo "mvn package error. Error message written to compileAfterGen_err${prefix}.txt"
    exit
fi
getres="mvn org.pitest:pitest-maven:mutationCoverage -DtargetClasses=original.${class_name} -DtargetTests=original.${class_name}_ESTest"
error_message=$( $getres > mvnexec_err${prefix}.txt 2>&1 )
if [ $? -eq 0 ]; then
    echo "Pitest execution successful"
    mv mvnexec_err${prefix}.txt pitAfter${prefix}.txt
else
    echo "Pitest execution failed. Error message written to mvnexec_err${prefix}.txt"
    exit
fi
mv target/pit-reports pitreports/${class_name}

echo "Cleaning..."
rm "${JAVA_SRC_DIR}"*
rm "${JAVA_SRCTEST_DIR}"*
mvn clean > /dev/null
