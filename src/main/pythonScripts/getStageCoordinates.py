import sys

# Check if any command-line arguments were provided
if len(sys.argv) == 1:
    # No arguments were passed, print default coordinates
    print('12345', '54321')
else:
    # Arguments were passed, print an error message to standard error
    print("Error: Unexpected arguments received.", file=sys.stderr)
    sys.exit(1)  # Optionally exit with a non-zero status to indicate an error
