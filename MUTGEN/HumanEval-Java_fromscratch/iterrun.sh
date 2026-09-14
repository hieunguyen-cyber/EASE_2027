#!/bin/bash
# Number of runs
num_runs=4

for i in $(seq 1 $num_runs); do
  echo "<--------------------------------------------Iteration $i running-------------------------------------------->"
  dir="iter${i}_log"
  echo $dir
  mkdir "$dir"
  ./autorun.sh
  mv *.txt "$dir"
  cp -r res "$dir"
  cp getResFromPITlog.sh "$dir"
  mv res/* iterRES/
  rm -rf pitreports/*
done
