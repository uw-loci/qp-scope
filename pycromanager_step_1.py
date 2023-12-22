import os
import sys
import shutil
import glob

# Function to log messages
def log_message(message):
    with open('D:/log.txt', 'a') as log_file:
        print(message, file=log_file, flush=True)

def copy_tif_files(projectsFolderPath, sampleLabel, imageType):
    
    if "4x" in imageType:
        TILES_LOCATION = 'C:/ImageAnalysis/Brightfield demo/Tiles/BurnTest2-4x-bf'
    else:
        TILES_LOCATION = 'C:/ImageAnalysis/Brightfield demo/Tiles/PDAC.3.A_Unstained-20x--C-7'

    log_message(f"Copying .tif files from {TILES_LOCATION} to {projectsFolderPath}/{sampleLabel}")

    dest_dir = os.path.join(projectsFolderPath, sampleLabel, imageType)
    log_message(f"Destination directory: {dest_dir}")
    if not os.path.exists(dest_dir):
        log_message("Destination directory does not exist, creating it.")
        os.makedirs(dest_dir)

    tif_files = []
    for extension in ['*.tif', '*.tiff', '*.txt']:
        tif_files.extend(glob.glob(os.path.join(TILES_LOCATION, extension)))

    log_message(f"Number of .tif files found: {len(tif_files)}")
    if not tif_files:
        log_message(f"No .tif files found in {TILES_LOCATION}")
        return False

    for file in tif_files:
        try:
            log_message(file)
            shutil.copy(file, dest_dir)
        except Exception as e:
            print(f"Error copying file {file}: {e}")


    return True

log_message("Python script started.")
projectsFolderPath = sys.argv[2]
log_message(f"Projects Folder Path: {projectsFolderPath}")
sampleLabel = sys.argv[3]
log_message(f"Sample Label: {sampleLabel}")
imageType = sys.argv[4]
log_message(f"Image Type: {imageType}")

success = copy_tif_files(projectsFolderPath, sampleLabel, imageType)

if not success:
    log_message("File copying did not complete successfully.")
else:
    log_message("File copying completed successfully.")


# import os
# import sys
# import shutil
# import glob

# # Redirecting print statements to a log file
# sys.stdout = open('logpython.txt', 'w')
# print(os.getcwd())
# def copy_tif_files(projectsFolderPath, sampleLabel, imageType):
    
#     if "4x" in imageType:
#         TILES_LOCATION = 'C:/ImageAnalysis/Brightfield demo/Tiles/BurnTest2-4x-bf'  # Replace with the absolute path
#     else:
#         TILES_LOCATION = 'C:/ImageAnalysis/Brightfield demo/Tiles/PDAC_MetroHealth_N352L42-20x--null'


#     print(f"Copying .tif files from {TILES_LOCATION} to {projectsFolderPath}/{sampleLabel}")

#     dest_dir = os.path.join(projectsFolderPath, sampleLabel, f"{imageType}{sampleLabel}")
#     print(f"Destination directory: {dest_dir}")
#     if not os.path.exists(dest_dir):
#         print("Destination directory does not exist, creating it.")
#         os.makedirs(dest_dir)

#     # Find all .tif files
#     tif_files = []
#     for extension in ['*.tif', '*.tiff', '*.txt']:
#         tif_files.extend(glob.glob(os.path.join(TILES_LOCATION, extension)))

#     print(f"Number of .tif files found: {len(tif_files)}")
#     #print(f"Files: {tif_files}")
#     # Check if any.tif files were found
#     if not tif_files:
#         print(f"No .tif files found in {TILES_LOCATION}")
#         return False
#     #Copy each .tif file
#     #shutil.copy(tif_files[0], dest_dir)
#     # print(f"Copying last file: {tif_files[-1]}")
#     # shutil.copy(tif_files[-1], dest_dir)

#     # for file in tif_files[:42]:
#     #     print(f"Copying file: {file}")
#     #     shutil.copy(file, dest_dir)

#     for file in tif_files:
#         try:
#             print(f"Copying file: {file}")
#             shutil.copy(file, dest_dir)
#         except Exception as e:
#             print(f"Error copying file {file}: {e}")

#     # print("Copy operation completed.")
#     return True


# # if len(sys.argv) != 9:
# #     print("Incorrect arguments for function: python script.py <projectsFolderPath> <sampleLabel> <imageType> <x1> <y1> <x2> <y2>")
# #     print(len(sys.argv))
# #     return
# print("Python script started.")
# projectsFolderPath = sys.argv[2]
# print(f"Projects Folder Path: {projectsFolderPath}")
# sampleLabel = sys.argv[3]
# print(f"Sample Label: {sampleLabel}")
# imageType = sys.argv[4]
# print(f"Image Type: {imageType}")
# # x1, y1, x2, y2 are received but not used in this script. They can be used if needed.

# #jsonFileLocation = sys.arv[9]
# #WOULD NEED FUNCTION HERE TO TRANSLATE COORDINATES TO STAGE COORDINATES

# success = copy_tif_files(projectsFolderPath, sampleLabel, imageType)

# if not success:
#     print("File copying did not complete successfully.")
# else:
#     print("File copying completed successfully.")

# # Close the log file
# sys.stdout.close()