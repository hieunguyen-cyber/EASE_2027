import sys
from bs4 import BeautifulSoup
from tree_sitter import Language, Parser
from tree_sitter_languages import get_language, get_parser

def get_java_class_name(java_code):
    """
    Extracts the class name from Java source code using Tree-sitter.
    """
    java_parser = get_parser("java")

    tree = java_parser.parse(java_code.encode("utf8"))
    root_node = tree.root_node

    class_names = []
    for node in root_node.children:
        if node.type == "class_declaration":
            class_name = java_code[node.children[1].start_byte : node.children[1].end_byte]
            class_names.append(class_name)

    return class_names

def extract_class_name_from_file(file_path):
    """Reads a Java file and extracts class names."""
    with open(file_path, "r", encoding="utf-8") as f:
        java_code = f.read()

    return get_java_class_name(java_code)

def parse_jacoco_report(html_file):
    """Parses the JaCoCo HTML report and extracts total branch coverage, missed lines, and total lines."""
    with open(html_file, "r", encoding="utf-8") as f:
        soup = BeautifulSoup(f, "html.parser")

    total_row = soup.find("tfoot").find("tr")

    if not total_row:
        raise ValueError("No 'Total' row found in the report.")

    total_data = total_row.find_all("td")

    missed_lines = int(total_data[7].text.strip().replace(",",""))  # Missed lines
    total_lines = int(total_data[8].text.strip().replace(",",""))   # Total lines
    branch_coverage = total_data[4].text.strip().replace(",","")   # Branch coverage (e.g., "100%")

    #return {
    #    "branch_coverage": branch_coverage,
    #    "missed_lines": missed_lines,
    #    "total_lines": total_lines
    #}
    return str(branch_coverage) + "," +str(1-missed_lines/total_lines) + "," + str(missed_lines) + "," + str(total_lines)

inputreport = sys.argv[1]
report_data = parse_jacoco_report(inputreport)
print(report_data)
