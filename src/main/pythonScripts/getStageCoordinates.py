import sys

print('12345', '54321')
# Check if any command-line arguments were provided

for arg in sys.argv:
    # Print each argument on a separate line
    print(arg)
    # No arguments were passed, print default coordinates

# else:
#     # Arguments were passed, print an error message to standard error
#     print("Error: Unexpected arguments received.", file=sys.stderr)
#     print('12345', '54321')
#     sys.exit(1)  # Optionally exit with a non-zero status to indicate an error
