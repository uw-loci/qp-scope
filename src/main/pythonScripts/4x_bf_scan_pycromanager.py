import os
import sys
import shutil
import glob
import time
from multiprocessing import Pool

def copy_file(file, dest_dir):
    try:
        shutil.copy(file, dest_dir)
        #print(f"Copied {file} to {dest_dir}")
        print(f"file saved {file}")
        #time.sleep(10)
    except Exception as e:
        print(f"Error copying file {file}: {e}")

def copy_tif_files(projectsFolderPath, sampleLabel, imageType, subregion):
    
    if "4x" in imageType:
        TILES_LOCATION = 'C:/ImageAnalysis/QPExtension0.5.0/data/sample_tiles/some_4x_data'
    else:
        TILES_LOCATION = 'C:/ImageAnalysis/QPExtension0.5.0/data/sample_tiles/some_20x_data'

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

    # Use multiprocessing to copy files in parallel
    with Pool() as pool:
        pool.starmap(copy_file, [(file, dest_dir) for file in tif_files])

    return True

def main():
    print("Python script started.")
    
    if len(sys.argv) == 5:
        projectsFolderPath = sys.argv[1]
        sampleLabel = sys.argv[2]
        imageType = sys.argv[3]
        subregion = sys.argv[4]
        if '[' in subregion and ']' in subregion:
            subregion = "bounds"
    else:
        # Assign default values
        projectsFolderPath = r"C:\ImageAnalysis\QPExtension0.5.0\data\slides"
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

if __name__ == '__main__':
    main()
