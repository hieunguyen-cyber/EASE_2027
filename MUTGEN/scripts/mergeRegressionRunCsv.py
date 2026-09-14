import csv
import sys
from collections import defaultdict

def merge_csvs(index_file, csv_files, output_file='merged.csv'):
    with open(index_file, 'r') as f:
        index_keys = [line.strip() for line in f if line.strip()]

    merged_data = defaultdict(list)

    for csv_file in csv_files:
        with open(csv_file, 'r') as f:
            reader = csv.reader(f)
            for row in reader:
                if not row: continue
                key, *values = row
                merged_data[key].extend(values)

    with open(output_file, 'w', newline='') as f:
        writer = csv.writer(f)
        for key in index_keys:
            writer.writerow([key] + merged_data.get(key, []))

    print(f"✅ Merged CSV saved as: {output_file}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python merge_script.py index.txt file1.csv file2.csv ...")
    else:
        merge_csvs(sys.argv[1], sys.argv[2:])
