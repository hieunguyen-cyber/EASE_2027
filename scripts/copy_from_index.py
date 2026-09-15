import sys
import shutil
from pathlib import Path

def copy_java_files(index_file_path, source_folder, destination_folder):
    destination_folder = Path(destination_folder)
    destination_folder.mkdir(parents=True, exist_ok=True)

    with Path(index_file_path).open(encoding="utf-8") as index_file:
        for line in index_file:
            prefix = line.strip()
            if not prefix:
                continue  

            relative_path = Path(*prefix.split(".")).with_suffix(".java")
            source_file_path = Path(source_folder) / relative_path

            if source_file_path.is_file():
                destination_file_path = destination_folder / source_file_path.name
                shutil.copy2(source_file_path, destination_file_path)
                print(f"Copied: {source_file_path} -> {destination_file_path}")
            else:
                print(f"File not found: {source_file_path}")

copy_java_files(sys.argv[1],sys.argv[2],sys.argv[3])
