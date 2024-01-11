import sys

# Function to log messages
def log_message(message):
    with open('D:/log.txt', 'a') as log_file:
        print(message, file=log_file, flush=True)

log_message("Python script started.")

# Check if there are exactly two command-line arguments
if len(sys.argv) == 3:
    try:
        # Parse the two arguments as doubles (X and Y)
        X = float(sys.argv[1])
        Y = float(sys.argv[2])
        log_message(f"X: {X}, Y: {Y}")
    except ValueError:
        log_message("Invalid arguments. Both X and Y must be doubles.")
else:
    log_message("Usage: python script.py <X> <Y>")
