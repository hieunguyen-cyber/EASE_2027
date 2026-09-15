import sys
import re
from pathlib import Path

test_dir = Path(sys.argv[1]).resolve()

pattern = r"(separateClassLoader\s*=\s*)true"

for file_path in test_dir.rglob("*ESTest.java"):
    content = file_path.read_text(encoding="utf-8")
    new_content = re.sub(pattern, r"\1false", content)
    if new_content != content:
        file_path.write_text(new_content, encoding="utf-8")
        print(f"Updated: {file_path.name}")
