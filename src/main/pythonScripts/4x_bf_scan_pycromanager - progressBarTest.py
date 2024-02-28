import time
import sys


def copy_tif_files(projectsFolderPath, sampleLabel, imageType, subregion):
    # Hardcoded number of times to simulate the copy operation
    number_of_copies = 100  # Set this to the desired number of "copy" messages
    
    print(f"Simulated copying .tif files to {projectsFolderPath}/{sampleLabel}/{subregion}")
    
    # Loop to simulate the copy operation
    for i in range(number_of_copies):
        print(f"Copied #{i+1}.tif")
        time.sleep(0.5)  # Delay between each simulated copy message

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
