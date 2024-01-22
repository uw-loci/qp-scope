package qupath.ext.qp_scope.utilities

class PythonTestScripts {

    static String pyTestGetStageCoordinates() {
        String getStageCoordinatesTest =
"""
import sys

print('12345', '54321')
# Check if any command-line arguments were provided

for arg in sys.argv:
    # Print each argument on a separate line
    print(arg)
    # No arguments were passed, print default coordinates
"""

        return getStageCoordinatesTest
    }

    static String pyTestSendStageCoordinates() {

        String sendStageCoordinatesTest =
"""
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

"""
        return sendStageCoordinatesTest
    }

    static String pyFauxMicroscopeAcquisition(){

        String microscopeAcquisitionTest =
"""
import os
import sys
import shutil
import glob

def copy_tif_files(projectsFolderPath, sampleLabel, imageType, subregion):
    
    if "4x" in imageType:
        TILES_LOCATION = 'C:/ImageAnalysis/QPExtensionTest/data/sample_tiles/some_4x_data'
    else:
        TILES_LOCATION = 'C:/ImageAnalysis/QPExtensionTest/data/sample_tiles/some_20x_data'

    print(f"Copying .tif files from {TILES_LOCATION} to {projectsFolderPath}/{sampleLabel}/{subregion}")

    dest_dir = os.path.join(projectsFolderPath, sampleLabel, imageType, subregion)
    print(f"Destination directory: {dest_dir}")
    if not os.path.exists(dest_dir):
        print("Destination directory does not exist, creating it.")
        os.makedirs(dest_dir)

    tif_files = []
    for extension in ['*.tif', '*.tiff', '*.txt']:
        tif_files.extend(glob.glob(os.path.join(TILES_LOCATION, extension)))

    print(f"Number of .tif files found: {len(tif_files)}")
    if not tif_files:
        print(f"No .tif files found in {TILES_LOCATION}")
        return False

    for file in tif_files:
        try:
            print(file)
            shutil.copy(file, dest_dir)
        except Exception as e:
            print(f"Error copying file {file}: {e}")

    return True

print("Python script started.")

# Check if sufficient arguments are provided, else use default values
if len(sys.argv) == 5:
    projectsFolderPath = sys.argv[1]
    sampleLabel = sys.argv[2]
    imageType = sys.argv[3]
    subregion = sys.argv[4]
        # Check if subregion is in the format of a bounding box
    if '[' in subregion and ']' in subregion:
        subregion = "bounds"
else:
    # Assign default values
    projectsFolderPath = r"C:\\ImageAnalysis\\QPExtensionTest\\data\\slides"
    sampleLabel = "First_Test"
    imageType = "4x_bf_1"
    subregion = "2914_1730"

print(f"Projects Folder Path: {projectsFolderPath}")
print(f"Sample Label: {sampleLabel}")
print(f"Image Type: {imageType}")
print(f"Subregion: {subregion}")

success = copy_tif_files(projectsFolderPath, sampleLabel, imageType, subregion)

if not success:
    print("File copying did not complete successfully.")
else:
    print("File copying completed successfully.")

"""
        return microscopeAcquisitionTest
    }

}
