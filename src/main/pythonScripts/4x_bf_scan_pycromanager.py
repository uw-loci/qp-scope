import time

def copy_tif_files(projectsFolderPath, sampleLabel, imageType, subregion):
    # Hardcoded number of times to simulate the copy operation
    number_of_copies = 100  # Set this to the desired number of "copy" messages
    
    print(f"Simulated copying .tif files to {projectsFolderPath}/{sampleLabel}/{subregion}")
    
    # Loop to simulate the copy operation
    for i in range(number_of_copies):
        print(f"Copied #{i+1}.tif")
        time.sleep(0.5)  # Delay between each simulated copy message

    return True
