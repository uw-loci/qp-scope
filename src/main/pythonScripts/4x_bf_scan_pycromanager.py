import os
import sys
import shutil
import glob

def copy_tif_files(projectsFolderPath, sampleLabel, imageType):
    
    if "4x" in imageType:
        TILES_LOCATION = 'C:/ImageAnalysis/QPExtensionTest/data/sample_tiles/some_4x_data'
    else:
        TILES_LOCATION = 'C:/ImageAnalysis/QPExtensionTest/data/sample_tiles/some_20x_data'

    print(f"Copying .tif files from {TILES_LOCATION} to {projectsFolderPath}/{sampleLabel}")

    dest_dir = os.path.join(projectsFolderPath, sampleLabel, imageType)
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
projectsFolderPath = sys.argv[2]
print(f"Projects Folder Path: {projectsFolderPath}")
sampleLabel = sys.argv[3]
print(f"Sample Label: {sampleLabel}")
imageType = sys.argv[4]
print(f"Image Type: {imageType}")

success = copy_tif_files(projectsFolderPath, sampleLabel, imageType)

if not success:
    print("File copying did not complete successfully.")
else:
    print("File copying completed successfully.")


# import os
# import sys
# import shutil
# import glob

# # Function to log messages
# def log_message(message):
#     with open('D:/log.txt', 'a') as log_file:
#         print(message, file=log_file, flush=True)

# def copy_tif_files(projectsFolderPath, sampleLabel, imageType):
    
#     if "4x" in imageType:
#         TILES_LOCATION = 'C:/ImageAnalysis/QPExtensionTest/data/sample_tiles/some_4x_data'
#     else:
#         TILES_LOCATION = 'C:/ImageAnalysis/QPExtensionTest/data/sample_tiles/some_20x_data'

#     log_message(f"Copying .tif files from {TILES_LOCATION} to {projectsFolderPath}/{sampleLabel}")

#     dest_dir = os.path.join(projectsFolderPath, sampleLabel, imageType)
#     log_message(f"Destination directory: {dest_dir}")
#     if not os.path.exists(dest_dir):
#         log_message("Destination directory does not exist, creating it.")
#         os.makedirs(dest_dir)

#     tif_files = []
#     for extension in ['*.tif', '*.tiff', '*.txt']:
#         tif_files.extend(glob.glob(os.path.join(TILES_LOCATION, extension)))

#     log_message(f"Number of .tif files found: {len(tif_files)}")
#     if not tif_files:
#         log_message(f"No .tif files found in {TILES_LOCATION}")
#         return False

#     for file in tif_files:
#         try:
#             log_message(file)
#             shutil.copy(file, dest_dir)
#         except Exception as e:
#             print(f"Error copying file {file}: {e}")

#     return True

# log_message("Python script started.")
# projectsFolderPath = sys.argv[2]
# log_message(f"Projects Folder Path: {projectsFolderPath}")
# sampleLabel = sys.argv[3]
# log_message(f"Sample Label: {sampleLabel}")
# imageType = sys.argv[4]
# log_message(f"Image Type: {imageType}")

# success = copy_tif_files(projectsFolderPath, sampleLabel, imageType)

# if not success:
#     log_message("File copying did not complete successfully.")
# else:
#     log_message("File copying completed successfully.")


