package qupath.ext.qp_scope.ui

import com.sun.javafx.collections.ObservableListWrapper

import javafx.scene.control.*
import javafx.scene.layout.GridPane
import org.slf4j.LoggerFactory
import qupath.ext.qp_scope.utilities.QPProjectFunctions
import qupath.ext.qp_scope.utilities.UtilityFunctions
import qupath.ext.qp_scope.utilities.MinorFunctions
import qupath.ext.qp_scope.utilities.TransformationFunctions
import qupath.lib.gui.QuPathGUI
import qupath.fx.dialogs.Dialogs
import qupath.lib.gui.scripting.QPEx
import qupath.lib.objects.PathObject
import qupath.lib.projects.Project
import qupath.lib.scripting.QP

import javafx.stage.Window

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import java.awt.geom.AffineTransform

import javafx.stage.Modality
import javafx.stage.Stage


class QP_scope_GUI {
    static final logger = LoggerFactory.getLogger(QP_scope_GUI.class)
    // GUI elements
    static TextField x1Field = new TextField("")
    static TextField y1Field = new TextField("")
    static TextField x2Field = new TextField("")
    static TextField y2Field = new TextField("")
    static TextField scanBox = new TextField("-13316,-1580,-14854,-8474")
    static preferences = QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems()
    static TextField groovyScriptDetectField = new TextField(AutoFillPersistentPreferences.getAnalysisScriptForAutomation());
    static TextField modalityField = new TextField(AutoFillPersistentPreferences.getModalityForAutomation());
    static TextField sampleLabelField = new TextField(AutoFillPersistentPreferences.getSlideLabel())
    static TextField classFilterField = new TextField(AutoFillPersistentPreferences.getClassList())

    static def extensionPath = preferences.find{it.getName() == "Extension Location"}.getValue().toString()
    static TextField groovyScriptField = new TextField(preferences.find{it.getName() == "Tissue Detection Script"}.getValue().toString())

    static TextField pixelSizeField = new TextField(AutoFillPersistentPreferences.getMacroImagePixelSizeInMicrons())
    static CheckBox nonIsotropicCheckBox = new CheckBox("Non-isotropic pixels")



    /*
        Test GUI elements for various sub-functions within the program that require everything to be running.
     */

    static void testGUI() {
        // Create the dialog
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setTitle("qp_scope")

        dlg.setOnShown(event -> {
            Window window = dlg.getDialogPane().getScene().getWindow();
            if (window instanceof Stage) {
                ((Stage) window).setAlwaysOnTop(true);
            }
        });
        // Set the content
        dlg.getDialogPane().setContent(createBoundingBoxInputDialog())

        // Add Okay and Cancel buttons
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

        // Preferences from GUI
        double frameWidth = preferences.find{it.getName() == "Camera Frame Width #px"}.getValue() as Double
        double frameHeight = preferences.find{it.getName() == "Camera Frame Height #px"}.getValue() as Double
        double pixelSizeSource = preferences.find{it.getName() == "Macro image px size"}.getValue() as Double
        double pixelSizeFirstImagingMode = preferences.find{it.getName() == "1st scan pixel size um"}.getValue() as Double
        double overlapPercent = preferences.find{it.getName() == "Tile Overlap Percent"}.getValue() as Double
        String projectsFolderPath = preferences.find{it.getName() == "Projects Folder"}.getValue() as String
        String virtualEnvPath =  preferences.find{it.getName() == "Python Environment"}.getValue() as String
        String pythonScriptPath =  preferences.find{it.getName() == "PycroManager Path"}.getValue() as String
        String compressionType = preferences.find{it.getName() == "Compression type"}.getValue() as String
        String tileHandling = preferences.find{it.getName() == "Tile Handling Method"}.getValue() as String
        boolean isSlideFlippedX = preferences.find{it.getName() == "Flip macro image X"}.getValue() as Boolean
        boolean isSlideFlippedY = preferences.find{it.getName() == "Flip macro image Y"}.getValue() as Boolean

        List<String> args = [projectsFolderPath,
                             "First_Test",
                             "4x_bf_1",
                             "2220_1738",
        ] as List<String>
        int count = MinorFunctions.countTifEntriesInTileConfig(args)
        logger.info("Count is $count")

//         Show the dialog and capture the response
        def result = dlg.showAndWait()

        // Handling the response
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Retrieve values from text fields
            def sampleLabel = sampleLabelField.getText()

            def x1 = x1Field.getText()
            def y1 = y1Field.getText()
            def x2 = x2Field.getText()
            def y2 = y2Field.getText()
            // Handle full bounding box input
            def boxString = scanBox.getText()
            //Boolean to check whether to proceed with running the microscope data collection
            boolean dataCheck = true
            // Continue with previous behavior using coordinates

            if (boxString != "") {
                def values = boxString.replaceAll("[^0-9.,]", "").split(",")
                if (values.length == 4) {
                    x1 = values[0]
                    y1 = values[1]
                    x2 = values[2]
                    y2 = values[3]
                }
            }
            if ([sampleLabel, x1, y1, x2, y2, virtualEnvPath, pythonScriptPath].any { it == null || it.isEmpty() }) {
                Dialogs.showWarningNotification("Warning!", "Incomplete data entered.")
                dataCheck = false
            }


            // Check if any value is empty

            if (dataCheck) {
                QuPathGUI qupathGUI = QPEx.getQuPath()

                def qp_test_coords_1 = [2350, 1088]
                def stage_test_coords_1 = [19594, 18295]
                def qp_test_coords_2 = [2565, 1088]
                def stage_test_coords_2  = [21108, 18295]
                def qp_test_coords_3 = [2352, 1726]
                def stage_test_coords_3  = [19550, 13715]
                def qp_test_coords_4 = [2566, 2045]
                def stage_test_coords_4  = [21130, 11571]
                def qp_test_list = [qp_test_coords_1,qp_test_coords_2, qp_test_coords_3, qp_test_coords_4].collect { it.collect { it as Double } }
                def stage_test_list = [stage_test_coords_1,stage_test_coords_2, stage_test_coords_3, stage_test_coords_4].collect { it.collect { it as Double } }

                for (int i = 0; i < qp_test_list.size(); i++) {
                    List<Double> qpTestCoords = qp_test_list[i]
                    List<Double> stageTestCoords = stage_test_list[i]

                    // Create initial scaling transform
                    AffineTransform transformation = new AffineTransform()
                    double scale =  (pixelSizeSource)/ pixelSizeFirstImagingMode
                    logger.info("scale is $scale")
                    transformation.scale(scale, -scale)
                    // Calculate the transformation for the current pair
                    // Calculate the offset in microns - the size of one frame in stage coordinates
                    double offsetX = 0.5 * frameWidth * pixelSizeFirstImagingMode;
                    double offsetY = 1 * frameHeight * pixelSizeFirstImagingMode;
                    def offset = [offsetX, offsetY]
                    transformation = TransformationFunctions.addTranslationToScaledAffine(
                            transformation,
                            qpTestCoords,
                            stageTestCoords,
                            offset)
                    logger.info("Transformation for pair ${i + 1}: $transformation")

                    // Apply the transformation to each test coordinate
                    qp_test_list.each { qpCoords ->
                        def transformedPoint = TransformationFunctions.QPtoMicroscopeCoordinates(qpCoords, transformation)
                        logger.info("Converted $qpCoords to $transformedPoint")
                        logger.info("Expected value was: ${stage_test_list[qp_test_list.indexOf(qpCoords)]}")
                    }
                }


            }
        }
    }
    /**
     * Launches a graphical user interface for macro image input in a microscopy imaging workflow. This function facilitates
     * the acquisition of necessary parameters from the user, such as coordinates, preferences, and paths for scripts and
     * environment setups. It validates user input, retrieves application preferences, executes scripts for tissue detection,
     * and manages tile configurations and stitching tasks. The GUI also handles affine transformation setup for image alignment
     * and coordinates the execution of Python scripts for microscope control and image processing.
     */
    static void macroImageInputGUI() {
        // Create the dialog
        //Or just check if image is open with annotations
        def dlg = createMacroImageInputDialog()

        // Show the dialog and capture the response
        Optional<ButtonType> result = dlg.showAndWait()

        // Handling the response
        if (result.isPresent() && result.get() == ButtonType.OK) {


            //TODO implement separate xy processing
            // Retrieve values from text fields and checkbox
            boolean arePixelsNonIsotropic = nonIsotropicCheckBox.isSelected()
            String groovyScriptPath = groovyScriptField.getText()
            def sampleLabel = sampleLabelField.getText()
            def pixelSizeMacroImage = pixelSizeField.getText()
            //Store this for future runs
            AutoFillPersistentPreferences.setMacroImagePixelSizeInMicrons(pixelSizeMacroImage)

            AutoFillPersistentPreferences.setSlideLabel(sampleLabel)
            // Preferences from GUI
            double frameWidth = preferences.find{it.getName() == "Camera Frame Width #px"}.getValue() as Double
            double frameHeight = preferences.find{it.getName() == "Camera Frame Height #px"}.getValue() as Double
            double pixelSizeSource = AutoFillPersistentPreferences.getMacroImagePixelSizeInMicrons() as Double
            double pixelSizeFirstImagingMode = preferences.find{it.getName() == "1st scan pixel size um"}.getValue() as Double
            double overlapPercent = preferences.find{it.getName() == "Tile Overlap Percent"}.getValue() as Double
            String projectsFolderPath = preferences.find{it.getName() == "Projects Folder"}.getValue() as String
            String virtualEnvPath =  preferences.find{it.getName() == "Python Environment"}.getValue() as String
            String pythonScriptPath =  preferences.find{it.getName() == "PycroManager Path"}.getValue() as String
            String compressionType = preferences.find{it.getName() == "Compression type"}.getValue() as String
            String tileHandling = preferences.find{it.getName() == "Tile Handling Method"}.getValue() as String
            boolean isSlideFlippedX = preferences.find{it.getName() == "Flip macro image X"}.getValue() as Boolean
            boolean isSlideFlippedY = preferences.find{it.getName() == "Flip macro image Y"}.getValue() as Boolean
            String firstImagingMode = preferences.find { it.getName() == "First Scan Type" }.getValue() as String
            //TODO add entry in GUI to get annotation labels
            def classLabels = ["Tissue"]
            // Log retrieved preference values
            logger.info("frameWidth: $frameWidth")
            logger.info("frameHeight: $frameHeight")
            logger.info("pixelSizeSource: $pixelSizeSource")
            logger.info("pixelSizeFirstImagingMode: $pixelSizeFirstImagingMode")
            logger.info("overlapPercent: $overlapPercent")
            logger.info("projectsFolderPath: $projectsFolderPath")
            logger.info("virtualEnvPath: $virtualEnvPath")
            logger.info("pythonScriptPath: $pythonScriptPath")
            logger.info("compressionType: $compressionType")

            // Check if data is all present
            if ([groovyScriptPath, projectsFolderPath, virtualEnvPath, pythonScriptPath, compressionType].any { it == null || it.isEmpty() }) {
                Dialogs.showWarningNotification("Warning!", "Insufficient data to send command to microscope!")
                return
            }
            //String imageName = QP.getCurrentImageName()

            Map scriptPaths = MinorFunctions.calculateScriptPaths(groovyScriptPath)
            String jsonTissueClassifierPathString = scriptPaths.jsonTissueClassfierPathString
            QuPathGUI qupathGUI = QPEx.getQuPath()
            Map projectDetails
            // Log input variables before entering the if-else block
            logger.info("Checking inputs before creating or retrieving project information:");
            logger.info("qupathGUI: " + qupathGUI);
            logger.info("projectsFolderPath: " + projectsFolderPath);
            logger.info("sampleLabel: " + sampleLabel);
            //create a projectDetails map with four values that will be needed later, all related to project creation.
            if (QPEx.getProject() == null) {
                projectDetails = QPProjectFunctions.createAndOpenQuPathProject(qupathGUI, projectsFolderPath, sampleLabel,
                        preferences as ObservableListWrapper, isSlideFlippedX, isSlideFlippedY)
            }else{
                //If the project already exists and an image is open, return that information
                projectDetails = QPProjectFunctions.getCurrentProjectInformation(projectsFolderPath, sampleLabel, firstImagingMode)
            }
            Project currentQuPathProject = projectDetails.currentQuPathProject as Project
            String tempTileDirectory = projectDetails.tempTileDirectory


            if (QP.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tissue")}.size() == 0) {
                String tissueDetectScript = UtilityFunctions.modifyTissueDetectScript(groovyScriptPath, pixelSizeSource as String, jsonTissueClassifierPathString)
                logger.info(tissueDetectScript)
                // Run the modified script
                //TODO runScript mistakenly treats the script as having a null language sometimes.
                qupathGUI.runScript(null, tissueDetectScript)
                //At this point the tissue should be outlined in an annotation
            }

            //Verify that the user is happy with the current annotations, then proceed with alignment and acquisition
            UI_functions.checkValidAnnotationsGUI(classLabels,{ boolean check ->
                if (!check) {
                    logger.info("Returned false from GUI status check checkValidAnnotationsGUI.")
                } else {

                    def annotations = QP.getAnnotationObjects().findAll { classLabels.contains(it.getPathClass().toString()) }
                    //Calculate the field of view size in QuPath pixels

                    Double frameWidthQPpixels = (frameWidth)* (pixelSizeFirstImagingMode / pixelSizeSource) //* pixelSizeFirstImagingMode
                    Double frameHeightQPpixels = (frameHeight) * (pixelSizeFirstImagingMode / pixelSizeSource) //* pixelSizeFirstImagingMode
                    UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, [firstImagingMode], "swap_objective_lens.py")
                    //Create tiles that represent individual fields of view along with desired overlap.
                    UtilityFunctions.performTilingAndSaveConfiguration(tempTileDirectory,
                            projectDetails.imagingModeWithIndex.toString(),
                            frameWidthQPpixels,
                            frameHeightQPpixels,
                            overlapPercent,
                            null,
                            true,
                            annotations)

                    //create a basic affine transformation, add the scaling information and a possible Y axis flip
                    //Then create a dialog that asks the user to select a single detection tile

                    AffineTransform scalingTransform = TransformationFunctions.setupAffineTransformationAndValidationGUI(pixelSizeSource as Double, preferences as ObservableListWrapper)

                    logger.info("Initial affine transform, scaling only: $scalingTransform")
                    //If user exited out of the dialog, the transformation should be null, and we do not want to continue.
                    if (scalingTransform == null) {
                        return
                    }

                    PathObject expectedTile = QP.getSelectedObject()
                    def detections = QP.getDetectionObjects()
                    def topCenterTileXY = TransformationFunctions.getTopCenterTile(detections)
                    def leftCenterTileXY = TransformationFunctions.getLeftCenterTile(detections)


                    //Get the coordinates of the upper left corner of the selected tile
                    List coordinatesQP = [expectedTile.getROI().getBoundsX(), expectedTile.getROI().getBoundsY()] as List<Double>
                    if (!coordinatesQP) {
                        logger.error("Need coordinates.")
                        return
                    }
                    logger.info("user adjusted position of tile at $coordinatesQP")
                    // Get the current stage coordinates to figure out the translation from the first alignment.
                    List<String> currentStageCoordinates_um_String = UtilityFunctions.runPythonCommand(virtualEnvPath,
                            pythonScriptPath,
                            null,
                            "getStageCoordinates.py")
                    logger.info("Obtained stage coordinates: $currentStageCoordinates_um_String")
                    logger.info("QuPath coordinates for selected tile: $coordinatesQP")
                    logger.info("affine transform before initial alignment: $scalingTransform")
                    List<Double> currentStageCoordinates_um = MinorFunctions.convertListToDouble(currentStageCoordinates_um_String)
                    //TODO
                    // PUT THIS INFORMATION SOMEWHERE ELSE
                    //Offset is required due to stage having a different point to define where the FOV is vs the centroid of the QuPath tile

                    double offsetX = -0.5 * frameWidth * (pixelSizeFirstImagingMode)
                    double offsetY = 0.5 * frameHeight * (pixelSizeFirstImagingMode)
                    def offset = [offsetX, offsetY]
                    AffineTransform transform = TransformationFunctions.addTranslationToScaledAffine(scalingTransform, coordinatesQP, currentStageCoordinates_um)
                    logger.info("affine transform after initial alignment: $scalingTransform")


                    // Handle stage alignment for top center tile
                    Map resultsTopCenter = UI_functions.handleStageAlignment(topCenterTileXY, qupathGUI, virtualEnvPath, pythonScriptPath, transform, offset)
                    if (!resultsTopCenter.updatePosition) {
                        logger.info("Window was closed, alignment cancelled.")
                        return // Exit if position validation fails
                    }
                    transform = resultsTopCenter.transformation as AffineTransform

                    // Handle stage alignment for left center tile
                    Map resultsLeftCenter = UI_functions.handleStageAlignment(leftCenterTileXY, qupathGUI, virtualEnvPath, pythonScriptPath, transform, offset)
                    if (!resultsLeftCenter.updatePosition) {
                        logger.info("Window was closed, alignment cancelled.")
                        return // Exit if position validation fails
                    }
                    transform = resultsLeftCenter.transformation as AffineTransform

                    //The TileConfiguration_QP.txt file is in QuPath pixel coordinates.
                    //It must be transformed into stage coordinates in microns
                    logger.info("export script path string $tempTileDirectory")


                    //Transformation here should be translating Qupath coordinates correctly into stage coordinates
                    //However we are getting camera pixel coordinates??


                    def tileconfigFolders = TransformationFunctions.transformTileConfiguration(tempTileDirectory, transform, offset)
                    for (folder in tileconfigFolders) {
                        logger.info("modified TileConfiguration at $folder")
                    }

                    Semaphore pythonCommandSemaphore = new Semaphore(1);
                    annotations.each { annotation ->

                        List<String> args = [projectsFolderPath,
                                             sampleLabel,
                                             projectDetails.imagingModeWithIndex,
                                             annotation.getName(),
                        ] as List<String>
                        logger.info("Check input args for runPythonCommand")

                        CompletableFuture<List<String>> pythonFuture = runPythonCommandAsync(virtualEnvPath, pythonScriptPath, args, pythonCommandSemaphore);


                        // Handle the successful completion of the Python command
                        pythonFuture.thenAcceptAsync(stageCoordinates -> {
                            // Process the result for successful execution
                            logger.info("Begin stitching")
                            String stitchedImagePathStr = UtilityFunctions.stitchImagesAndUpdateProject(
                                    projectsFolderPath,
                                    sampleLabel,
                                    projectDetails.imagingModeWithIndex as String,
                                    annotation.getName(),
                                    qupathGUI,
                                    currentQuPathProject,
                                    compressionType,
                                    pixelSizeFirstImagingMode,
                            1);
                            logger.info("Stitching completed at $stitchedImagePathStr")
                            // Ensure stitching operation is also non-blocking and async
                        }).exceptionally(throwable -> {
                            // Handle any exceptions from the Python command
                            logger.error("Error during Python script execution: ${throwable.message}")
                            UI_functions.notifyUserOfError("Error during Python script execution: ${throwable.message}", "Python Script Execution")
                            return null; // To comply with the Function interface return type
                        });
                    }

                    logger.info("All collections complete, tiles will be handled as $tileHandling")
                    //Check if the tiles should be deleted from the collection folder set
                    //tempTileDirectory is the parent folder to each annotation/bounding folder

                    if (tileHandling == "Delete")
                        UtilityFunctions.deleteTilesAndFolder(tempTileDirectory)
                    if (tileHandling == "Zip") {
                        UtilityFunctions.zipTilesAndMove(tempTileDirectory)
                        UtilityFunctions.deleteTilesAndFolder(tempTileDirectory)
                    }
                }
            })
        }
        logger.info("check3")
    }

    /**
     * Initiates a graphical user interface for inputting a bounding box to create a tiling grid.
     * The function collects inputs from the user regarding the bounding box coordinates or a full bounding box specification.
     * It then processes these inputs to set up and execute a microscopy scan based on the specified area.
     * This includes creating a QuPath project, configuring tiling parameters, running a Python script for data collection,
     * and optionally handling the stitched image and tile management post-scanning.
     */
    static void boundingBoxInputGUI() {
        // Create the dialog
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setOnShown(event -> {
            Window window = dlg.getDialogPane().getScene().getWindow();
            if (window instanceof Stage) {
                ((Stage) window).setAlwaysOnTop(true);
            }
        });
        dlg.setTitle("qp_scope")
        //dlg.setHeaderText("Enter details (LOOK MA! " + BasicStitchingExtension.class.getName() + "!):");

        // Set the content
        dlg.getDialogPane().setContent(createBoundingBoxInputDialog())

        // Add Okay and Cancel buttons
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

        // Show the dialog and capture the response
        def result = dlg.showAndWait()

        // Handling the response
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Retrieve values from text fields
            def sampleLabel = sampleLabelField.getText()
            def x1 = x1Field.getText()
            def y1 = y1Field.getText()
            def x2 = x2Field.getText()
            def y2 = y2Field.getText()
            // Handle full bounding box input
            def boxString = scanBox.getText()
            //Boolean to check whether to proceed with running the microscope data collection
            boolean dataCheck = true

            //Preferences from GUI
            double frameWidth = preferences.find{it.getName() == "Camera Frame Width #px"}.getValue() as Double
            double frameHeight = preferences.find{it.getName() == "Camera Frame Height #px"}.getValue() as Double
            double pixelSizeFirstImagingMode = preferences.find{it.getName() == "1st scan pixel size um"}.getValue() as Double
            double overlapPercent = preferences.find{it.getName() == "Tile Overlap Percent"}.getValue() as Double
            boolean isInvertedYProperty = preferences.find{it.getName() == "Flip macro image Y"}.getValue() as Boolean
            boolean isInvertedXProperty = preferences.find{it.getName() == "Flip macro image X"}.getValue() as Boolean
            String projectsFolderPath = preferences.find{it.getName() == "Projects Folder"}.getValue() as String
            String virtualEnvPath =  preferences.find{it.getName() == "Python Environment"}.getValue() as String
            String pythonScriptPath =  preferences.find{it.getName() == "PycroManager Path"}.getValue() as String
            String compressionType = preferences.find{it.getName() == "Compression type"}.getValue() as String
            String tileHandling = preferences.find{it.getName() == "Tile Handling Method"}.getValue() as String
            String firstImagingMode = preferences.find { it.getName() == "First Scan Type" }.getValue() as String
            // Continue with previous behavior using coordinates
            if (boxString != "") {
                def values = boxString.replaceAll("[^0-9.,]", "").split(",")
                if (values.length == 4) {
                    x1 = values[0]
                    y1 = values[1]
                    x2 = values[2]
                    y2 = values[3]
                }
            }
            // Check if any needed values are missing
            if ([sampleLabel, x1, y1, x2, y2, virtualEnvPath, pythonScriptPath].any { it == null || it.isEmpty() }) {
                Dialogs.showWarningNotification("Warning!", "Incomplete data entered.")
                return
            }


            QuPathGUI qupathGUI = QPEx.getQuPath()
            Map projectDetails = QPProjectFunctions.createAndOpenQuPathProject(qupathGUI, projectsFolderPath, sampleLabel, preferences as ObservableListWrapper)
            Project currentQuPathProject = projectDetails.currentQuPathProject as Project
            String tempTileDirectory = projectDetails.tempTileDirectory
            String imagingModeWithIndex = projectDetails.imagingModeWithIndex

            //Specifically for the case where there is only a bounding box provided
            List<Double> boundingBoxValues = [x1, y1, x2, y2].collect { it.toDouble() }
            Double frameWidthMicrons = (frameWidth ) * (pixelSizeFirstImagingMode )
            Double frameHeightMicrons = (frameHeight )  * (pixelSizeFirstImagingMode )
            //Input frame width/height in microns since everything is done in stage coordinates
            UtilityFunctions.performTilingAndSaveConfiguration(tempTileDirectory, imagingModeWithIndex,
                    frameWidthMicrons,
                    frameHeightMicrons,
                    overlapPercent,
                    boundingBoxValues,
                    false,
                    [], //No annotations for a bounding box
                    isInvertedYProperty,
                    isInvertedXProperty,
            )
            UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, [firstImagingMode], "swap_objective_lens.py")
            //Send the scanning command to the microscope

            List args = [projectsFolderPath,
                         sampleLabel,
                         imagingModeWithIndex,
                         "bounds"]

            UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, args, null)

            // Handle image stitching and update project

            String stitchedImagePathStr = UtilityFunctions.stitchImagesAndUpdateProject(projectsFolderPath,
                    sampleLabel, imagingModeWithIndex, "bounds", qupathGUI, currentQuPathProject,
                    compressionType, pixelSizeFirstImagingMode, 1)
            logger.info(stitchedImagePathStr)

            qupathGUI.refreshProject()
            //Check if the tiles should be deleted from the collection folder

            if (tileHandling == "Delete")
                UtilityFunctions.deleteTilesAndFolder(tempTileDirectory)
            if (tileHandling == "Zip") {
                UtilityFunctions.zipTilesAndMove(tempTileDirectory)
                UtilityFunctions.deleteTilesAndFolder(tempTileDirectory)
            }

        }
    }

/**
 * Creates and returns a GridPane containing input fields for specifying a bounding box.
 * This includes fields for entering the coordinates of the top-left (X1, Y1) and bottom-right (X2, Y2) corners of the bounding box,
 * as well as an option for entering a full bounding box specification. It is designed to facilitate user input in the boundingBoxInputGUI function.
 *
 * @return GridPane with labeled input fields for bounding box specification.
 */
    private static GridPane createBoundingBoxInputDialog() {
        GridPane pane = new GridPane()
        pane.setHgap(10)
        pane.setVgap(10)
        def row = 0

        // Add new component for Sample Label
        UI_functions.addToGrid(pane, new Label('Sample Label:'), sampleLabelField, row++)

        // Add existing components to the grid
        UI_functions.addToGrid(pane, new Label('X1:'), x1Field, row++)
        UI_functions.addToGrid(pane, new Label('Y1:'), y1Field, row++)
        UI_functions.addToGrid(pane, new Label('X2:'), x2Field, row++)
        UI_functions.addToGrid(pane, new Label('Y2:'), y2Field, row++)
        UI_functions.addToGrid(pane, new Label('Full bounding box:'), scanBox, row++)

        return pane
    }

/**
 * Launches a GUI dialog for initiating a secondary imaging mode data collection process within selected annotations of the current image.
 * The function gathers user input from the GUI to specify parameters for the collection, such as sample label and class filters for annotations.
 * It then calculates necessary parameters based on user preferences and initiates the data collection process for each selected annotation.
 * The process includes tiling, running a Python command for data collection, and optionally stitching the collected data.
 * Post-collection, it handles the tiles according to user preference, including deletion or compression.
 */
    static void secondImagingModeGUI() {
        //TODO check if in a project?
        def logger = LoggerFactory.getLogger(QuPathGUI.class)
        // Create the dialog
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setOnShown(event -> {
            Window window = dlg.getDialogPane().getScene().getWindow();
            if (window instanceof Stage) {
                ((Stage) window).setAlwaysOnTop(true);
            }
        });
        dlg.setTitle("Collect image data from an annotated subset of your current image.")
        dlg.setHeaderText("Create annotations within your image, then click Okay to proceed with a second collection within those areas.")

        // Set the content
        dlg.getDialogPane().setContent(createSecondImagingModeDialog())

        // Add Okay and Cancel buttons
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

        // Show the dialog and capture the response
        def result = dlg.showAndWait()

        // Handling the response
        logger.info("Starting processing GUI output")
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Retrieve values from text fields
            def sampleLabel = sampleLabelField.getText()
            //Store the current value for the next run
            AutoFillPersistentPreferences.setClassList(classFilterField.getText())

            def classFilter = classFilterField.getText().split(',').collect { it.trim() }


            //Preferences from GUI
            double frameWidth = preferences.find { it.getName() == "Camera Frame Width #px" }.getValue() as Double
            double frameHeight = preferences.find { it.getName() == "Camera Frame Height #px" }.getValue() as Double
            double pixelSizeFirstImagingMode = preferences.find { it.getName() == "1st scan pixel size um" }.getValue() as Double
            double pixelSizeSecondImagingMode = preferences.find { it.getName() == "2nd scan pixel size um" }.getValue() as Double
            double overlapPercent = preferences.find { it.getName() == "Tile Overlap Percent" }.getValue() as Double
            String projectsFolderPath = preferences.find { it.getName() == "Projects Folder" }.getValue() as String
            String virtualEnvPath = preferences.find { it.getName() == "Python Environment" }.getValue() as String
            String pythonScriptPath = preferences.find { it.getName() == "PycroManager Path" }.getValue() as String
            String compressionType = preferences.find { it.getName() == "Compression type" }.getValue() as String
            String tileHandling = preferences.find { it.getName() == "Tile Handling Method" }.getValue() as String
            String secondImagingMode = preferences.find { it.getName() == "Second Scan Type" }.getValue() as String

            //SETUP: collect variables
            QuPathGUI qupathGUI = QPEx.getQuPath()
            //String imagingModeWithIndex = MinorFunctions.getUniqueFolderName(projectsFolderPath + File.separator + sampleLabel + File.separator + secondImagingMode)
            //TODO CHANGE TO ACCESS CURRENT IMAGING MODE
            logger.info("Imaging modality: $secondImagingMode")
            Map projectDetails = QPProjectFunctions.getCurrentProjectInformation(projectsFolderPath, sampleLabel, secondImagingMode)
            Project currentQuPathProject = projectDetails.currentQuPathProject as Project
            String tempTileDirectory = projectDetails.tempTileDirectory
            //Boolean to check whether to proceed with running the microscope data collection
            logger.info("getting annotation objects contained in $classFilter ")
            def annotations = QP.getAnnotationObjects()
            annotations = annotations.findAll { classFilter.contains(it.getPathClass().toString()) }

            // Check if we have sufficient information to proceed
            if (annotations.isEmpty() || [sampleLabel, virtualEnvPath, pythonScriptPath].any { it == null || it.isEmpty() }) {
                Dialogs.showWarningNotification("Warning!", "Insufficient data to send command to microscope!")
                return
            }
            UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, [secondImagingMode], "swap_objective_lens.py")
            //Callback that was removed - need to re-insert the checkValidAnnotations function here
            UI_functions.checkValidAnnotationsGUI(classFilter,{ boolean check ->
                if (!check) {
                    logger.info("Returned false from GUI status check checkValidAnnotationsGUI.")
                } else {

                    //Calculate the field of view size in QuPath pixels
                    Double frameWidthQPpixels = (frameWidth)* (pixelSizeSecondImagingMode / pixelSizeFirstImagingMode) //* (pixelSizeSecondImagingMode)
                    Double frameHeightQPpixels = (frameHeight)* (pixelSizeSecondImagingMode / pixelSizeFirstImagingMode) //* (pixelSizeSecondImagingMode)
                    logger.info("Frame width in pixels within a QuPath 20x image should be about a quarter of a 4x tile, or $frameWidthQPpixels")
                    //Create tiles that represent individual fields of view along with desired overlap.
                    //Remove previous tiles
                    QP.clearDetections()
                    UtilityFunctions.performTilingAndSaveConfiguration(tempTileDirectory,
                            projectDetails.imagingModeWithIndex.toString(),
                            frameWidthQPpixels,
                            frameHeightQPpixels,
                            overlapPercent,
                            null,
                            true,
                            annotations)

                    //create a basic affine transformation, add the scaling information and a possible Y axis flip
                    //Then create a dialog that asks the user to select a single detection tile

                    AffineTransform scalingTransform = TransformationFunctions.setupAffineTransformationAndValidationGUI( pixelSizeFirstImagingMode as Double,
                            preferences as ObservableListWrapper)

                    logger.info("Initial affine transform, scaling only: $scalingTransform")
                    //If user exited out of the dialog, the transformation should be null, and we do not want to continue.
                    if (scalingTransform == null) {
                        return
                    }

                    PathObject expectedTile = QP.getSelectedObject()
                    def detections = QP.getDetectionObjects()
                    def topCenterTileXY = TransformationFunctions.getTopCenterTile(detections)
                    def leftCenterTileXY = TransformationFunctions.getLeftCenterTile(detections)


                    // Get the current stage coordinates to figure out the translation from the first alignment.
                    List coordinatesQP = [expectedTile.getROI().getBoundsX(), expectedTile.getROI().getBoundsY()] as List<Double>
                    if (!coordinatesQP) {
                        logger.error("Need coordinates.")
                        return
                    }
                    logger.info("user adjusted position of tile at $coordinatesQP")
                    List<String> currentStageCoordinates_um_String = UtilityFunctions.runPythonCommand(virtualEnvPath,
                            pythonScriptPath,
                            null,
                    "getStageCoordinates.py")
                    logger.info("Obtained stage coordinates: $currentStageCoordinates_um_String")
                    logger.info("QuPath coordinates for selected tile: $coordinatesQP")
                    logger.info("affine transform before initial alignment: $scalingTransform")
                    List<Double> currentStageCoordinates_um = MinorFunctions.convertListToDouble(currentStageCoordinates_um_String)
                    //TODO
                    // PUT THIS INFORMATION SOMEWHERE ELSE
                    //Offset is required due to stage having a different point to define where the FOV is (not upper left)
                    double offsetX = -0.5 * frameWidth * (pixelSizeSecondImagingMode)
                    double offsetY = 0.5 * frameHeight * (pixelSizeSecondImagingMode)
                    // Create the offset AffineTransform
                    def offset = [offsetX, offsetY]
                    AffineTransform transform = TransformationFunctions.addTranslationToScaledAffine(scalingTransform,
                            coordinatesQP,
                            currentStageCoordinates_um,
                            offset)
                    logger.info("affine transform after initial alignment: $scalingTransform")


                    // Handle stage alignment for top center tile
                    Map resultsTopCenter = UI_functions.handleStageAlignment(topCenterTileXY,
                            qupathGUI,
                            virtualEnvPath,
                            pythonScriptPath,
                            transform,
                            offset)
                    if (!resultsTopCenter.updatePosition) {
                        logger.info("Window was closed, alignment cancelled.")
                        return // Exit if position validation fails
                    }
                    transform = resultsTopCenter.transformation as AffineTransform

                    // Handle stage alignment for left center tile
                    Map resultsLeftCenter = UI_functions.handleStageAlignment(leftCenterTileXY, qupathGUI, virtualEnvPath, pythonScriptPath, transform, offset)
                    if (!resultsLeftCenter.updatePosition) {
                        logger.info("Window was closed, alignment cancelled.")
                        return // Exit if position validation fails
                    }
                    transform = resultsLeftCenter.transformation as AffineTransform

                    //The TileConfiguration_QP.txt file is in QuPath pixel coordinates.
                    //It must be transformed into stage coordinates in microns
                    logger.info("export script path string $tempTileDirectory")


                    //Transformation here should be translating Qupath coordinates correctly into stage coordinates
                    //However we are getting camera pixel coordinates??


                    def tileconfigFolders = TransformationFunctions.transformTileConfiguration(tempTileDirectory, transform, offset)
                    for (folder in tileconfigFolders) {
                        logger.info("modified TileConfiguration at $folder")
                    }

                    Semaphore pythonCommandSemaphore = new Semaphore(1);
                    annotations.each { annotation ->

                        List<String> args = [projectsFolderPath,
                                             sampleLabel,
                                             projectDetails.imagingModeWithIndex,
                                             annotation.getName(),
                        ] as List<String>
                        logger.info("Check input args for runPythonCommand")

                        CompletableFuture<List<String>> pythonFuture = runPythonCommandAsync(virtualEnvPath, pythonScriptPath, args, pythonCommandSemaphore);


                        // Handle the successful completion of the Python command
                        pythonFuture.thenAcceptAsync(stageCoordinates -> {
                            // Process the result for successful execution
                            logger.info("Begin stitching")
                            String stitchedImagePathStr = UtilityFunctions.stitchImagesAndUpdateProject(
                                    projectsFolderPath,
                                    sampleLabel,
                                    projectDetails.imagingModeWithIndex as String,
                                    annotation.getName(),
                                    qupathGUI,
                                    currentQuPathProject,
                                    compressionType,
                                    pixelSizeSecondImagingMode,
                            1);
                            logger.info("Stitching completed at $stitchedImagePathStr")
                            // Ensure stitching operation is also non-blocking and async
                        }).exceptionally(throwable -> {
                            // Handle any exceptions from the Python command
                            logger.error("Error during Python script execution: ${throwable.message}")
                            UI_functions.notifyUserOfError("Error during Python script execution: ${throwable.message}", "Python Script Execution")
                            return null; // To comply with the Function interface return type
                        });
                    }

                    logger.info("All collections complete, tiles will be handled as $tileHandling")
                    //Check if the tiles should be deleted from the collection folder set
                    //tempTileDirectory is the parent folder to each annotation/bounding folder

                    if (tileHandling == "Delete")
                        UtilityFunctions.deleteTilesAndFolder(tempTileDirectory)
                    if (tileHandling == "Zip") {
                        UtilityFunctions.zipTilesAndMove(tempTileDirectory)
                        UtilityFunctions.deleteTilesAndFolder(tempTileDirectory)
                    }
                }
            })
        }
    }


/**
 * Creates and configures a dialog for inputting macro image settings, including sample label and tissue detection script paths.
 * The dialog is designed to collect configuration settings for macro view imaging, ensuring that it stays on top of other windows
 * for better visibility and interaction. The content of the dialog is populated with a dynamically created GUI
 * for input fields related to macro image settings.
 *
 * @return The configured Dialog instance ready for showing to the user.
 */

    private static Dialog<ButtonType> createMacroImageInputDialog() {
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setOnShown(event -> {
            Window window = dlg.getDialogPane().getScene().getWindow();
            if (window instanceof Stage) {
                ((Stage) window).setAlwaysOnTop(true);
            }
        });
        dlg.setTitle("Macro View Configuration")
        dlg.setHeaderText("Configure settings for macro view.")
        dlg.getDialogPane().setContent(createMacroImageDialogContent())
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)
        // Accessing the Window of the Dialog to set always on top
        dlg.setOnShown(event -> {
            Window window = dlg.getDialogPane().getScene().getWindow();
            if (window instanceof Stage) {
                ((Stage) window).setAlwaysOnTop(true);
            }
        });
        return dlg
    }

/**
 * Constructs a GridPane layout containing input fields and labels for configuring macro image settings.
 * This method dynamically adds UI components to a GridPane for collecting settings such as the sample label,
 * the path to a tissue detection script, and pixel size. It is designed to be used as the content of a dialog
 * or another container within a GUI. Each row of the grid contains a different setting, organized for clarity and ease of use.
 *
 * @return A GridPane containing the configured UI components for macro image input settings.
 */

    private static GridPane createMacroImageDialogContent() {
        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);
        int row = 0;

        UI_functions.addToGrid(pane, new Label('Sample Label:'), sampleLabelField, row++)

        // Tissue Detection Script Field
        UI_functions.addToGrid(pane, new Label('Tissue detection script:'), groovyScriptField, row++)

        // Checkbox for Automatic Second Modality Scan
        CheckBox modalityCheckBox = new CheckBox("Automatic second modality scan");
        pane.add(modalityCheckBox, 0, row++, 2, 1);

        // Additional fields for Groovy script and modality
        Label groovyScriptLabel = new Label("Groovy script to detect ROIs:");

        Label modalityLabel = new Label("Modality:");


        // Add fields to the grid but set them initially invisible
        pane.add(groovyScriptLabel, 0, row);
        pane.add(groovyScriptDetectField, 1, row++);
        pane.add(modalityLabel, 0, row);
        pane.add(modalityField, 1, row++);

        // Binding visibility of fields to the checkbox state
        groovyScriptLabel.visibleProperty().bind(modalityCheckBox.selectedProperty());
        groovyScriptDetectField.visibleProperty().bind(modalityCheckBox.selectedProperty());
        modalityLabel.visibleProperty().bind(modalityCheckBox.selectedProperty());
        modalityField.visibleProperty().bind(modalityCheckBox.selectedProperty());

        return pane;
    }




    //Create the second interface window for performing higher resolution or alternate modality scans
    private static GridPane createSecondImagingModeDialog() {
        GridPane pane = new GridPane()
        pane.setHgap(10)
        pane.setVgap(10)
        def row = 0

        // Add new component for Sample Label
        UI_functions.addToGrid(pane, new Label('Sample Label:'), sampleLabelField, row++)
        UI_functions.addToGrid(pane, new Label('Annotation classes to image:'), classFilterField, row++)

        // Listener for the checkbox

        return pane
    }


    static CompletableFuture<List<String>> runPythonCommandAsync(String virtualEnvPath, String pythonScriptPath, List<String> args, Semaphore pythonCommandSemaphore) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                pythonCommandSemaphore.acquire(); // Ensure only one command runs at a time
                return UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, args);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null; // Handle this appropriately
            } finally {
                pythonCommandSemaphore.release();
            }
        });
    }


}
