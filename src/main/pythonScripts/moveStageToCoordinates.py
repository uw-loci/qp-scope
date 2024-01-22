import sys

# Check if there are exactly two command-line arguments
if len(sys.argv) == 3:
    try:
        # Parse the two arguments as doubles (X and Y)
        X = float(sys.argv[1])
        Y = float(sys.argv[2])
        print(f"X: {X}, Y: {Y}")
    except ValueError:
        print("Invalid arguments. Both X and Y must be doubles.", file=sys.stderr)
        sys.exit(1)
else:
    print("Expected two arguments, X and Y as doubles", file=sys.stderr)
    sys.exit(1)
