from pycromanager import Core, Studio
from smartpath_libraries.sp_acquisition import SPAcquisition
import yaml
from skimage import io, img_as_uint, transform
import os
import numpy as np
import glob
import pathlib
import shutil
import sys
import json

#print(sys.path)
cwd = os.getcwd()
os.chdir(os.path.dirname(os.path.realpath(__file__)))

if len(sys.argv) > 2:  
    print("Arguments passed, using them", sys.argv)  
    #tile_configuration_folder name of the final folder that contains a TileConfiguration_transformed.txt
    #The full path is then projectsFolderPath/sampleLabel/scan_type/tile_configuration_folder/TileConfiguration_transformed.txt
    self_filename, projectsFolderPath, sampleLabel, scan_type, tile_configuration_folder = sys.argv
    qupath_project_folder = os.path.join(projectsFolderPath, scan_type+sampleLabel)    
else:
    print("No arguments passed, using default values")    
    self_filename = r'C:\Users\lociuser\Codes\smart-wsi-scanner\minimal_qupathrunner.py'
    #projectsFolderPath =  r'D:\ImageAnalysis\slides' 
    projectsFolderPath =  r'C:\Users\lociuser\Codes\MikeN\data\slides'
    sampleLabel = 'First_Test'
    scan_type = '4x_bf_1' 
    tile_configuration_folder  = 'null'

tile_config_path = os.path.join(qupath_project_folder, tile_configuration_folder, "TileConfiguration_transformed.txt")

# Initialize an empty list to store the coordinates
coordinates = []

# Open the file and read the contents
with open(tile_config_path, 'r') as file:
    next(file)  # Skip the first line
    for line in file:
        # Extract coordinates using regular expression
        match = re.search(r'\((\d+), (\d+)\)', line)
        if match:
            x, y = map(float, match.groups())
            coordinates.append([x, y])

# At this point, `coordinates` is a list of [x, y] pairs
# You can later add z values to each pair as needed
# TODO FIX overall workflow to directly work off of the coordinates
            
try:
    def init_pycromanager():
        core = Core()
        studio = Studio()
        core.set_timeout_ms(20000)
        print("Pycromanager loaded successfully")
        return core, studio


    ## Config Loading
    config = yaml.load(open("config.yaml", "r"), Loader=yaml.FullLoader)
    config = {**config["user_config"], **config["model_config"], **config["hard_config"]}
    lsm_pixel_size_base = config["pixel-size-shg-base"]
    bf_4x_pixel_size_base = config["pixel-size-bf-4x"]
    bf_20x_pixel_size_base = config["pixel-size-bf-20x"]
    camera_resolution_base = config["camera-resolution"]

    ## User configuration block
    save_path = os.path.join(projectsFolderPath,"acquisition") #"data/acquisition"

    #slide_box = [5000, 9000, 9000, 14000]  # [bottom left corner, and top right corner]
    #slide_box = [25000, 16000, 28000, 20000] ## for the panc #360-50 slide loaded on 1/11/2024

    print("Coordiantes", coordinates)
    brightfield_4x_background_fname = (
        "data/presets/BG_4x.tiff"  # give a default 4x background image
    )
    brightfield_20x_background_fname = (
        "data/presets/BG_20x.tiff"  # give a default 20x background image
    )

    core, studio = init_pycromanager()
    
    sp_acq = SPAcquisition(
        config=config,
        mmcore=core,
        mmstudio=studio,
        bf_4x_bg=io.imread(brightfield_4x_background_fname),
        bf_20x_bg=io.imread(brightfield_20x_background_fname),
    )

    #TODO Figure out how to change slide box to the positions lists.

    sp_acq.update_slide_box(slide_box) #Not sure what this is if there is no slide box
    position_list_4x = sp_acq.generate_grid(mag="4x", overlap=50) #I think this should be replaced with coordinates?
    sp_acq.position_list_4x = position_list_4x

    acq_name = sampleLabel + "-4x-bf"
    sp_acq.switch_objective(mag="4x")
    sp_acq.switch_mod(mod="bf")

    sp_acq.update_focus_presets(mag="4x", mod="bf")  # update focus preset
    sp_acq.config[
        "autofocus-speed"
    ] = 4  # default is 4 ## `1-6`, the larger the faster, but potentially worse autofocusing resuls.

    core.set_shutter_open(True)
    core.set_auto_shutter(False)
    core.set_shutter_open(True)

    print("starting Acquisition")
    results_4x = sp_acq.whole_slide_bf_scan(
        save_path,
        acq_name,
        position_list_4x.reshape(position_list_4x.shape[0] * position_list_4x.shape[1], -1),
        mag="4x",
        focus_dive=True,
        estimate_background=False,
    )

    acq_id = len(glob.glob(os.path.join(save_path, acq_name + "*")))
    acq_path = os.path.join(save_path, acq_name + "_{}".format(acq_id))
    print("Saved files to {}".format(acq_path))

    position_list = position_list_4x
    if position_list.ndim == 3:
        p1, p2, p3 = position_list.shape
        position_list = np.reshape(position_list, [p1 * p2, p3])

    pixel_size = config["pixel-size-bf-4x"]

    image_list = glob.glob(os.path.join(acq_path, "*.tif"))
    stitchfolder_path = os.path.join(acq_path, "stitch")
    if not os.path.exists(stitchfolder_path):
        os.mkdir(stitchfolder_path)

    assert len(position_list) == len(
        image_list
    ), "Number of images does not match number of positions"


    with open(os.path.join(stitchfolder_path, "TileConfiguration.txt"), "w") as text_file:
        print("dim = {}".format(2), file=text_file)
        for pos in range(position_list.shape[0]):
            x = int(position_list[pos][0] / pixel_size)
            y = int(position_list[pos][1] / pixel_size)
            print("{}.tif; ; ({}, {})".format(pos, x, y), file=text_file)

    # shutil.copy(
    #     os.path.join(stitchfolder_path, "TileConfiguration.txt"),
    #     os.path.join(stitchfolder_path, "TileCoordinates.txt"),
    # )

    for pos in range(len(image_list)):
        fn = image_list[pos]
        fname = pathlib.Path(fn).name

        img = io.imread(fn)

        correction = False
        rotate = False
        flip_y = True
        flip_x = False

        # if correction is True and background_image is not None:
        #    img = white_balance(img, background_image)
        #    img = flat_field(img, bg_img)

        if rotate is not None:
            img = transform.rotate(img, rotate)

        if flip_y:
            img = img[::-1, :]

        if flip_x:
            img = img[:, ::-1]

    ##TODO: replace scikit iosave with tifffile with metadata
        io.imsave(
            stitchfolder_path + "/{}.tif".format(pos),
            img_as_uint(img),
            check_contrast=False,
        )

    qupath_stitching_folder = os.path.join(projectsFolderPath,sampleLabel,scan_type)
    print(f"copying from \n{stitchfolder_path} \t to \n{qupath_stitching_folder}")

    shutil.copytree(stitchfolder_path,
                    qupath_stitching_folder,dirs_exist_ok=True)


    print("Finished saving tiles for stitching at", stitchfolder_path)
    os.chdir(cwd)

    del studio
    del core

except json.decoder.JSONDecodeError:
    print("bounding_box is not a list or a string")
