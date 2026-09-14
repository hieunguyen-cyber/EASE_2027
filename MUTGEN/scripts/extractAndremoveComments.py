import os
import shutil
import re
import sys
from pathlib import Path
import openai
from ollama_config import OLLAMA_API_KEY, OLLAMA_MODEL, OLLAMA_OPENAI_BASE_URL

client = openai.OpenAI(
    base_url=OLLAMA_OPENAI_BASE_URL,
    api_key=OLLAMA_API_KEY,
)
def replace_mutation_content(file_path, replacement_string):
    with open(file_path, 'r') as file:
        content = file.read()
    updated_content = re.sub(r'(## Source code description)(.*?)(##)', r'\1\n' + replacement_string + r'\3', content, flags=re.DOTALL)
    with open(file_path, 'w') as file:
        file.write(updated_content)


def extract_strings(source_code):
    """Extracts all string literals from the Java source code without modifying the original content."""
    string_literals = re.findall(r'"(.*?)"', source_code)
    return string_literals

def summarize_code_from_file(file_path):
    """Summarizes the Java source code while retaining comments and extracting string literals."""
    with open(file_path, "r") as f:
        source_code = f.read()
    
    extracted_strings = extract_strings(source_code)
    
    prompt = (
        f"Summarize the following Java source code in 1-3 sentences, including its purpose "
        f"and input format (if applicable)."
        f"```java\n{source_code}\n```"
    )
    
    response = client.chat.completions.create(
        model=OLLAMA_MODEL,
        messages=[{"role": "user", "content": prompt}]
    )

    return response.choices[0].message.content
def remove_comments_and_empty_lines(input_file):
    with open(input_file, 'r') as infile:
        lines = infile.readlines()

    cleaned_lines = []
    inside_block_comment = False

    for line in lines:
        stripped_line = line.lstrip()  # Remove leading spaces for comment detection
        indent = line[:len(line) - len(stripped_line)]  # Preserve original indentation

        if inside_block_comment:
            if '*/' in stripped_line:
                inside_block_comment = False
                after_comment = stripped_line.split('*/', 1)[1].lstrip()
                if after_comment:  # If there's remaining code after `*/`, keep it
                    line = indent + after_comment
                else:
                    continue  # If nothing meaningful remains, skip the line
            else:
                continue  # Skip entire line inside a block comment

        line = re.sub(r'//.*', '', line)

        while '/*' in line:
            before_comment = line.split('/*', 1)[0].rstrip()
            after_comment = line.split('/*', 1)[1]

            if '*/' in after_comment:  # Handle inline block comment
                after_comment = after_comment.split('*/', 1)[1].lstrip()
                line = before_comment + " " + after_comment if before_comment or after_comment else ""
            else:
                inside_block_comment = True
                line = before_comment  # Keep only the part before `/*`

        if re.fullmatch(r'\s*\*/\s*', line):
            continue

        line = line.rstrip()

        if line.strip():  # Only keep non-empty lines
            cleaned_lines.append(line)

    with open(input_file, 'w') as outfile:
        for line in cleaned_lines:
            outfile.write(line + '\n')

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 extractAndremoveComments.py <input_file>")
        sys.exit(1)

    input_file = sys.argv[1]
    input_file_name = input_file.split('/')[-1]
    
    summary = summarize_code_from_file(input_file)  + "\n"
    print(f'{summary}')
    remove_comments_and_empty_lines(input_file)
    default_prompt = (
        Path(__file__).resolve().parents[1]
        / "mutahunter/core/templates/test_generation/test_generator_user.txt"
    )
    file_path = os.getenv("MUTGEN_TEST_GENERATOR_PROMPT", str(default_prompt))
    replace_mutation_content(file_path,summary)


if __name__ == "__main__":
    main()
