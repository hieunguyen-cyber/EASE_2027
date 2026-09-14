import os
import re

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as file:
        content = file.read()

    # Step 1: Add import after 'package original;'
    content = re.sub(
        r'(package\s+original;\s*)',
        r'\1import java.util.*;\n',
        content
    )

    # Step 2: Locate outermost { ... }
    brace_start = content.find('{')
    brace_end = content.rfind('}')
    
    if brace_start != -1 and brace_end != -1 and brace_start < brace_end:
        # Step 3: Insert @Test method inside the braces
        insertion = '\n    @Test\n    public void test0() throws Throwable {\n    }\n'
        content = content[:brace_start+1] + insertion + content[brace_end:]

    # Step 4: Write back the file
    with open(filepath, 'w', encoding='utf-8') as file:
        file.write(content)

def process_directory(directory):
    for root, _, files in os.walk(directory):
        for filename in files:
            if filename.endswith('ESTest.java'):
                filepath = os.path.join(root, filename)
                print(f'Processing: {filepath}')
                process_file(filepath)

# Example usage
process_directory('./iterRES')
