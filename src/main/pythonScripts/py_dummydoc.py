import os
import sys

def find_next_filename():
    counter = 1
    filename = f"dummy{counter}.txt"
    while os.path.exists(filename):
        counter += 1
        filename = f"dummy{counter}.txt"
    print(filename)
    return filename

def create_file(filename, args):
    with open(filename, 'w') as file:
        file.write("Arguments passed:\n")
        for arg in args:
            file.write(f"{arg}\n")

# Find the next available filename
next_filename = find_next_filename()

# Get command line arguments (excluding the script name)
arguments = sys.argv[1:]

# Create the file and write the arguments to it
create_file(next_filename, arguments)
print("Anything goes here")
