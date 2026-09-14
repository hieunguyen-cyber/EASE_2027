import sys

def merge_logs(log1_path, log2_path, output_path):
    with open(log1_path, 'r') as file1, open(log2_path, 'r') as file2:
        log1_lines = [line.strip().split(',') for line in file1]
        log2_lines = {line.strip().split(',')[0]: line.strip().split(',')[1:] for line in file2}
    with open(output_path, 'w') as output_file:
        for line in log1_lines:
            A, rest = line[0], line[1:]
            if A in log2_lines:
                output_file.write(','.join(line + log2_lines[A]) + '\n')
            else:
                output_file.write(','.join(line) + '\n')
                print(line)

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python mergeCovms.py <covlog> <mslog> <output>")
    else:
        merge_logs(sys.argv[1], sys.argv[2], sys.argv[3])

