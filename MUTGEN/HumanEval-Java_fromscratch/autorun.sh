#!/bin/bash

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
MUTGEN_ROOT=$(cd -- "${SCRIPT_DIR}/.." && pwd)

JAVA_SRC_DIR="src/main/java/original/"
JAVA_SRCTEST_DIR="src/test/java/original/"
PITEST_REPORT_DIR="oriReport/pit-reports/original/"
TEST_USER_PROMPT_FILE="${MUTGEN_ROOT}/mutahunter/core/templates/test_generation/test_generator_user.txt"
ANALYZER_USER_PROMPT_FILE="${MUTGEN_ROOT}/mutahunter/core/templates/test_generation/analyzer_user.txt"

GETNAME_SCRIPT="../scripts/getClassName.py"
COMMENT_PROCESSING_SCRIPT="../scripts/extractAndremoveComments.py"
MUTATIONREPORT_PROCESSING_SCRIPT="../scripts/parsePITestReport.py"

BACKUP_JAVA_SRC_DIR="backup/${JAVA_SRC_DIR}"
RES_DIR="res/"

# Path to your index file
csv_file="../index_humaneval_java.csv"
target_files=()
while IFS= read -r line; do
  for id in $line; do
    target_files+=("${id}.java")
  done
done < "$csv_file"
target_files_set="${target_files[*]}"
timeout_duration=900

if [ "$(ls -A pitreports/)" ]; then
    echo "pitreport/ NOT empty!"
    exit 1
fi
if [ "$(ls -A "${RES_DIR}")" ]; then
    echo "res/ NOT empty!"
    exit 1
fi
if [ "$(ls -A "${JAVA_SRC_DIR}")" ]; then
    rm "${JAVA_SRC_DIR}"*
fi
if [ "$(ls -A "${JAVA_SRCTEST_DIR}")" ]; then
    rm "${JAVA_SRCTEST_DIR}"*
fi
mvn clean > /dev/null

# Find all Java files and process them one by one
find "$BACKUP_JAVA_SRC_DIR" -type f -name "*.java" | while read -r java_file; do
    file_name=$(basename "$java_file")
    if [[ $target_files_set =~ $file_name ]]; then
        cp "${BACKUP_JAVA_SRC_DIR}${file_name}" "${JAVA_SRC_DIR}"
        java_file="${JAVA_SRC_DIR}${file_name}"
        echo "Processing: $java_file"
    else
        continue
    fi
    #cp "${BACKUP_JAVA_SRC_DIR}${file_name}" "${JAVA_SRC_DIR}"
    #java_file="${JAVA_SRC_DIR}${file_name}"
    #echo "Processing: $java_file"
    timeout $timeout_duration ./runeach.sh "$java_file"
    if [ $? -eq 124 ]; then
        echo "Timeout: $java_file"
    fi
done
