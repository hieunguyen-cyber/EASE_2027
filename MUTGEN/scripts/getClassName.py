from tree_sitter import Language, Parser
from tree_sitter_languages import get_language, get_parser

def get_java_class_name(java_code):
    """
    Extracts the class name from Java source code using Tree-sitter.
    """
    # Get Java parser
    java_parser = get_parser("java")

    # Parse the Java source code
    tree = java_parser.parse(java_code.encode("utf8"))
    root_node = tree.root_node

    # Find class declaration node
    class_names = []
    for node in root_node.children:
        if node.type == "class_declaration":
            # Get the class name from its first child (identifier)
            class_name = java_code[node.children[1].start_byte : node.children[1].end_byte]
            class_names.append(class_name)

    return class_names

def extract_class_name_from_file(file_path):
    """Reads a Java file and extracts class names."""
    with open(file_path, "r", encoding="utf-8") as f:
        java_code = f.read()

    return get_java_class_name(java_code)

if __name__ == "__main__":
    import sys
    if len(sys.argv) != 2:
        print("Usage: python find_class.py <java_file>")
        sys.exit(1)

    file_path = sys.argv[1]
    class_names = extract_class_name_from_file(file_path)

    if class_names:
        #print(f"Class Names: {', '.join(class_names)}")
        print(f"{', '.join(class_names)}")
    else:
        print("No class declaration found.")
