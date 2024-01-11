import sys

# Function to log messages
def log_message(message):
    with open('D:/log.txt', 'a') as log_file:
        print(message, file=log_file, flush=True)

log_message("Python script started.")

# Check if the first command-line argument is None
if len(sys.argv) >= 2:
    first_argument = sys.argv[1]
    if first_argument is None:
        log_message("The first argument is None.")
else:
    log_message("No arguments were provided.")
print('12345', '54321')