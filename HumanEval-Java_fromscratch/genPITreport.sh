#!/bin/bash
JAVA_SRC_DIR="src/main/java/original/"
GETNAME_SCRIPT="../scripts/getClassName.py"
RES_DIR="res/"

timeout_duration=200

FOLDER="pitreports"
if [ "$(ls -A "$FOLDER")" ]; then
    echo "$FOLDER is not empty. Exiting..."
    exit 1
fi

# Find all Java files and process them one by one
find "$JAVA_SRC_DIR" -type f -name "*.java" | while read -r java_file; do
    class_name=$(python3 "$GETNAME_SCRIPT" "$java_file" 2>/dev/null)
    echo "Processing ${class_name},$java_file......"
    timeout $timeout_duration mvn org.pitest:pitest-maven:mutationCoverage -DtargetClasses=original.${class_name} -DtargetTests=original.${class_name}_ESTest
    if [ $? -eq 124 ]; then
        echo "Timeout: $java_file"
    fi
    mv target/pit-reports pitreports/${class_name}
done
