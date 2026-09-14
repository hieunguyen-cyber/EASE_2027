import sys
import os
import re

test_dir = sys.argv[1]

pattern = r"(separateClassLoader\s*=\s*)true"

for dirpath,_,filenames in os.walk(test_dir):
  for filename in filenames:
    if filename.endswith("ESTest.java"):
        file_path = os.path.join(dirpath, filename)

        with open(file_path, "r", encoding="utf-8") as file:
            content = file.read()

        new_content = re.sub(pattern, r"\1false", content)

        if new_content != content:
            with open(file_path, "w", encoding="utf-8") as file:
                file.write(new_content)
            print(f"Updated: {filename}")
