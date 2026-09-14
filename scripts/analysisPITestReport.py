import re
import sys
import openai
from bs4 import BeautifulSoup
from ollama_config import OLLAMA_API_KEY, OLLAMA_MODEL, OLLAMA_OPENAI_BASE_URL

client = openai.OpenAI(
    base_url=OLLAMA_OPENAI_BASE_URL,
    api_key=OLLAMA_API_KEY,
)

def replace_mutation_content(file_path, replacement_string):
    # Read the file content
    with open(file_path, 'r') as file:
        content = file.read()
    # Use regex to find the block between "## Mutation Report" and the next "##" and replace it
    updated_content = re.sub(r'(## Mutation Report)(.*?)(##)', r'\1\n' + replacement_string + r'\3', content, flags=re.DOTALL)
    # Write the updated content back to the file
    with open(file_path, 'w') as file:
        file.write(updated_content)


# Function to send prompt to OpenAI LLM and get mutant code description
def get_mutant_type(description):
    prompt = f"""
    Given the following mutation description:

    Mutation Description: {description}

    Please tell me, which type is the mutation? please pick one from the following types:
    Conditionals Boundary, Increments, Invert Negatives, Math, Negate Conditionals, Void Method Calls, Empty returns, False Returns, True returns, Null returns, Primitive returns
    Please only reply the type, do not contain any other words.

    """

    response = client.chat.completions.create(
        model=OLLAMA_MODEL,
        messages=[
            {"role": "system", "content": "You are a mutation tool PITest profession that helps with understanding code mutations."},
            {"role": "user", "content": prompt}
        ]
    )

    return response.choices[0].message.content

def parse_pitest_report(html_file):
    """Parses the PITest HTML report to extract mutations, line numbers, and statuses."""
    
    with open(html_file, "r", encoding="utf-8") as f:
        soup = BeautifulSoup(f, "html.parser")
    
    mutations_section = soup.find("h2", string="Mutations")  # Find the 'Mutations' section
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
            mutation_cells = row.find_all("p", class_=["NO_COVERAGE", "SURVIVED", "KILLED"])
            
            for mutation in mutation_cells:
                # number = mutation.find("b").text.strip() if mutation.find("b") else ""
                pop_spans = mutation.find_all("span")
                if pop_spans:
                    testc = [pop_spans[0].find_next_sibling(string=True)]
                    description = testc[0].strip()
                    mutype = get_mutant_type(description)
                    mutation_action = description.split("→")[0].strip()
                    tp = description.split("→")[1].strip()

                mutations.append(f"{mutation_action} at line {line_number}, {tp}, {mutype}")

    return mutations

inputreport = sys.argv[1]
report_data = parse_pitest_report(inputreport) 
mutation_prompt = [f"{i+1}. {item}" for i, item in enumerate(report_data)]
print(mutation_prompt)
