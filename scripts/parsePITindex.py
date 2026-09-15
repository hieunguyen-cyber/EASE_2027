import sys
import re
from pathlib import Path
from bs4 import BeautifulSoup

def parse_index_html(file_path):
    """Parses an index.html file to extract class name, mutation coverage, killed mutants, and total mutants."""
    soup = BeautifulSoup(Path(file_path).read_text(encoding="utf-8"), "html.parser")

    results = []
    
    table_heading = next((h3 for h3 in soup.find_all("h3") if "Breakdown by" in h3.text), None)
    
    if table_heading:
        table = table_heading.find_next("table")
    else:
        return results  # No valid table found
    
    for row in table.find_all("tr")[1:]:  # Skip the header row
        cols = row.find_all("td")
        if len(cols) < 1:
            continue  # Skip invalid rows

        class_name = re.sub(r"\.java$", "", cols[0].text.strip())  # Extract class name
        coverage_bars = row.find_all("div", class_="coverage_bar")
        if len(coverage_bars) < 2:
            continue  # Skip if not enough coverage bars found
        
        mutation_coverage_div = coverage_bars[1]  # Second coverage bar is mutation coverage
        mutation_coverage_text = mutation_coverage_div.find_previous_sibling("div", class_="coverage_percentage").text.strip()  # e.g., "90%"
        mutants_data = mutation_coverage_div.find("div", class_="coverage_legend").text.strip()  # e.g., "18/20"
        
        killed_mutants, total_mutants = map(int, mutants_data.split("/"))
        
        results.append(f"{class_name},{mutation_coverage_text},{killed_mutants},{total_mutants}")
    
    return results

def find_and_parse_logs(log_folder):
    """Recursively finds all index.html files in a folder and extracts mutation coverage data."""
    output = []
    
    for file_path in Path(log_folder).rglob("index.html"):
        output.extend(parse_index_html(file_path))
    
    return output

log_folder = sys.argv[1]
results = find_and_parse_logs(log_folder)
for line in results:
  if 'original' not in line:
    print(line)
