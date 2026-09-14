import sys
import os
import shutil

def copy_java_files(index_file_path, source_folder, destination_folder):
    os.makedirs(destination_folder, exist_ok=True)

    with open(index_file_path, 'r') as index_file:
        for line in index_file:
            prefix = line.strip()
            if not prefix:
                continue  

            relative_path = prefix.replace('.', os.sep) + '.java'
            source_file_path = os.path.join(source_folder, relative_path)

            if os.path.isfile(source_file_path):
                destination_file_path = os.path.join(destination_folder, os.path.basename(source_file_path))
                shutil.copy2(source_file_path, destination_file_path)
                print(f"Copied: {source_file_path} -> {destination_file_path}")
            else:
                print(f"File not found: {source_file_path}")

copy_java_files(sys.argv[1],sys.argv[2],sys.argv[3])
