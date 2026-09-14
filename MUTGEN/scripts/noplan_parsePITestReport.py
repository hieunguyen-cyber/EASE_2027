import os
import re
import sys
from pathlib import Path
import openai
from bs4 import BeautifulSoup
from ollama_config import OLLAMA_API_KEY, OLLAMA_MODEL, OLLAMA_OPENAI_BASE_URL

client = openai.OpenAI(
    base_url=OLLAMA_OPENAI_BASE_URL,
    api_key=OLLAMA_API_KEY,
)

def replace_mutation_content(file_path, replacement_string):
    with open(file_path, 'r') as file:
        content = file.read()
    updated_content = re.sub(r'(## Mutation Report)(.*?)(##)', r'\1\n' + replacement_string + r'\3', content, flags=re.DOTALL)
    with open(file_path, 'w') as file:
        file.write(updated_content)


def get_mutant_description_and_code(description, source_code_file):
    match = re.match(r"(.+) at line (\d+)", description)
    mutation_description_match = match.group(1)
    line_number_match = match.group(2)
    
    if not line_number_match or not mutation_description_match:
        raise ValueError("Description must contain a line number and mutation description.")

    line_number = int(line_number_match)
    mutation_description = mutation_description_match.strip()
    
    with open(source_code_file, "r", encoding="utf-8", errors="ignore") as file:
        lines = file.readlines()

    mutant_code = lines[int(line_number) - 1].strip()

    prompt = f"""
    Given the following source code file and the mutation description:

    Source Code:
    {''.join(lines)}

    Mutation Description: {description}
    code to be mutated: {lines[line_number-1]}

    Please only retain the part code that has been mutated in your response, do not contain any '''java format.
    """

    response = client.chat.completions.create(
        model=OLLAMA_MODEL,
        messages=[
            {"role": "system", "content": "You are an assistant that helps with understanding code mutations."},
            {"role": "user", "content": prompt}
        ]
    )

    return response.choices[0].message.content

def parse_pitest_report(html_file):
    """Parses the PITest HTML report to extract mutations, line numbers, and statuses."""
    
    with open(html_file, "r", encoding="utf-8") as f:
        soup = BeautifulSoup(f, "html.parser")
    
    mutations_section = soup.find("h2", text="Mutations")  # Find the 'Mutations' section
    if not mutations_section:
        raise ValueError("No 'Mutations' section found in the report.")
    
    mutations = []

    # Locate the 'Mutations' part table rows
    rows = mutations_section.find_parent("table").find_all("tr")

    for row in rows:
        # Find the <a> tag that holds the line number
        line_number_tag = row.find("a", href=True)
        
        if line_number_tag:
            line_number = line_number_tag.text.strip()
            mutation_cells = row.find_all("p", class_=["NO_COVERAGE", "SURVIVED"])
            
            for mutation in mutation_cells:
                # number = mutation.find("b").text.strip() if mutation.find("b") else ""
                pop_spans = mutation.find_all("span")
                if pop_spans:
                    testc = [pop_spans[0].find_next_sibling(text=True)]
                    description = testc[0].strip()
                    mutation_action = description.split("→")[0].strip()

                mutations.append(f"{mutation_action} at line {line_number}")

    return mutations

inputreport = sys.argv[1]
inputsrc = sys.argv[2]
report_data = parse_pitest_report(inputreport) 
mutation_prompt = [f"{i+1}. {item}" for i, item in enumerate(report_data)]
output = "Analyze the live mutant to think how to find the defects, the generated test cases must look very different with the existing ones.\n"
if not mutation_prompt:
    print("No available live mutants.")
for md in mutation_prompt:
    output += str(md) + '\n'
    mutant = get_mutant_description_and_code(md, inputsrc)
    output += str(mutant) + '\n'
print(output)
default_prompt = (
    Path(__file__).resolve().parents[1]
    / "mutahunter/core/templates/test_generation/test_generator_user.txt"
)
file_path = os.getenv("MUTGEN_TEST_GENERATOR_PROMPT", str(default_prompt))
replace_mutation_content(file_path,output+"\n")
