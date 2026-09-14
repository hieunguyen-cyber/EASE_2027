#!/bin/bash
GETNAME_SCRIPT="../scripts/getClassName.py"
JAVA_SRC_DIR="src/main/java/original/"
PARSECOV_SCRIPT="../scripts/parseJaCoCoReport.py"

find "$JAVA_SRC_DIR" -type f -name "*.java" | while read -r java_file; do
    prefix=$(basename "$java_file" .java)
    report_file="$prefix.java.html"
    
    # get class name from java src file
    class_name=$(python "$GETNAME_SCRIPT" "$java_file" 2>/dev/null)
    if [ -n "$class_name" ]; then # TODO: catch exception
        :
    else
        echo "No class found in: $java_file"
        continue
        #exit
    fi

    getres=$(python "$PARSECOV_SCRIPT" "target/site/jacoco/original/${class_name}.html" "$java_file" 2>/dev/null)
    echo "$getres"
done
