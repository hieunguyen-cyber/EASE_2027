#!/bin/bash

before_files=$(ls pitBefore*.txt 2>/dev/null)
after_files=$(ls pitAfter*.txt 2>/dev/null)

# Create associative arrays to track the core names
declare -A before_map
declare -A after_map

# Extract core names from filenames and store them
for file in $before_files; do
    core=${file#pitBefore}
    core=${core%.txt}
    before_map["$core"]=$file
done

for file in $after_files; do
    core=${file#pitAfter}
    core=${core%.txt}
    after_map["$core"]=$file
done

# Find matching pairs
for core in "${!before_map[@]}"; do
    if [[ -n "${after_map[$core]}" ]]; then
        before_file=${before_map[$core]}
        after_file=${after_map[$core]}
        file="$before_file"
        core_name=$(basename "$file" | sed -E 's/^pit(Before|After)//; s/\.txt$//')
        log_info=$(tail -n 15 "$file")
        mutation_score_before=$(echo "$log_info" | grep -oP 'Killed \d+ \(\K[0-9]+%' | head -n 1)
        num_mutants=$(echo "$log_info" | grep -oP 'Generated \K\d+(?= mutations)' | head -n 1)
        
        file="$after_file"
        core_name=$(basename "$file" | sed -E 's/^pit(Before|After)//; s/\.txt$//')
        log_info=$(tail -n 15 "$file")
        mutation_score_after=$(echo "$log_info" | grep -oP 'Killed \d+ \(\K[0-9]+%' | head -n 1)
        echo "$core_name, $mutation_score_before, $mutation_score_after,$num_mutants"
    fi
done
