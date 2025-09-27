import logging
import re

# --- Setup logging ---
logging.basicConfig(
    filename="detected_strings.log",
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)

def detect_and_log(filepath: str, search_pattern: str):
    """
    Scan a file line by line, detect a string/pattern, and log matches.
    :param filepath: Path to the file to scan
    :param search_pattern: String or regex pattern to search
    """
    regex = re.compile(search_pattern)

    with open(filepath, "r", encoding="utf-8") as f:
        for line_no, line in enumerate(f, start=1):
            if regex.search(line):
                message = f"Match found in line {line_no}: {line.strip()}"
                print(message)  # Optional console output
                logging.info(message)


if __name__ == "__main__":
    # Example usage
    file_to_scan = "application.log"   # Replace with your log file
    pattern = r"ERROR"                 # Example: detect "ERROR"
    detect_and_log(file_to_scan, pattern)
