package qupath.ext.qp_scope.functions

import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.stage.Modality
import org.slf4j.LoggerFactory
import qupath.ext.basicstitching.stitching.StitchingImplementations
import qupath.ext.qp_scope.utilities.UtilityFunctions
import qupath.ext.qp_scope.utilities.MinorFunctions
import qupath.ext.qp_scope.utilities.TransformationFunctions
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.gui.scripting.QPEx
import qupath.lib.objects.PathObject
import qupath.lib.projects.Project
import qupath.lib.projects.ProjectImageEntry
import qupath.lib.scripting.QP

import java.awt.geom.Point2D
import java.util.concurrent.Semaphore
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

import java.nio.file.Path
import java.nio.file.Paths

import java.util.stream.Collectors

import static qupath.lib.scripting.QP.getAnnotationObjects
import static qupath.lib.scripting.QP.getSelectedObject
import static qupath.lib.scripting.QP.project

//Thoughts:
//Have collapsible sections to a larger dialog box?
//        Alternatively, individual dialogs for each step, but have menu options for each as well. The menu options for steps not yet reached would need to be greyed out.

class QP_scope_GUI {
    static final logger = LoggerFactory.getLogger(QP_scope_GUI.class)
    // GUI elements
    static TextField x1Field = new TextField("")
    static TextField y1Field = new TextField("")
    static TextField x2Field = new TextField("")
    static TextField y2Field = new TextField("")
    static TextField scanBox = new TextField("0,0,5000,5000")
    static preferences = UtilityFunctions.getPreferences()
    static TextField virtualEnvField = new TextField(preferences.environment)
    static TextField pythonScriptField = new TextField(preferences.pycromanager)
    static TextField projectsFolderField = new TextField(preferences.projects)
    static TextField sampleLabelField = new TextField("First_Test")
    static TextField classFilterField = new TextField("Tumor, Immune, PDAC")
    static CheckBox slideFlippedCheckBox = new CheckBox("Slide is flipped")
    static TextField groovyScriptField = new TextField(preferences.extensionPath+"/src/main/groovyScripts/DetectTissue.groovy")

    static TextField pixelSizeField = new TextField(preferences.pixelSizeSource)
    static CheckBox nonIsotropicCheckBox = new CheckBox("Non-isotropic pixels")



        /**********************************
     * Starting point for an overview or "macro" image
     */

    static void testGUI() {
        // Create the dialog
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setTitle("qp_scope")
        //dlg.setHeaderText("Enter details (LOOK MA! " + BasicStitchingExtension.class.getName() + "!):");

        // Set the content
        dlg.getDialogPane().setContent(createBoundingBoxInputGUI())

        // Add Okay and Cancel buttons
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

        // Show the dialog and capture the response
        def result = dlg.showAndWait()

        // Handling the response
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Retrieve values from text fields
            def sampleLabel = sampleLabelField.getText()
            def virtualEnvPath = virtualEnvField.getText()
            def pythonScriptPath = pythonScriptField.getText()
            def projectsFolderPath = projectsFolderField.getText()
            def x1 = x1Field.getText()
            def y1 = y1Field.getText()
            def x2 = x2Field.getText()
            def y2 = y2Field.getText()
            // Handle full bounding box input
            def boxString = scanBox.getText()
            //Boolean to check whether to proceed with running the microscope data collection
            boolean dataCheck = true
            def pixelSize = preferences.pixelSizeFirstScanType

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
//                AffineTransform scalingTransform = AffineTransform.getScaleInstance(0.153472, -0.153472) // Example scaling transform
//                List<String> qpCoordinatesList = ["2003.3333740234375", "1094.4444580078125"] // Example qpCoordinates
//                List<String> stageCoordinatesList = ["-12490.77", "-1936.179"] // Example stageCoordinates
//
//                AffineTransform transform = TransformationFunctions.initialTransformation(scalingTransform, qpCoordinatesList, stageCoordinatesList)
//                println("AffineTransform: " + transform)
//                // Apply the transformation to the original qpCoordinates and validate the output
//                double[] qpPoint = qpCoordinatesList.collect { it.toDouble() } as double[]
//                Point2D.Double transformedPoint = applyTransformation(transform, qpPoint)
//                logger.info("Transformed qpPoint using the AffineTransform: ${transformedPoint}")
                UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, [-13316, -5027.2])
                sleep(5000)
                UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, [-11893, -1227.2])

            }
        }
    }
    static Point2D.Double applyTransformation(AffineTransform transform, double[] point) {
        Point2D.Double originalPoint = new Point2D.Double(point[0], point[1])
        Point2D.Double transformedPoint = new Point2D.Double()
        transform.transform(originalPoint, transformedPoint)
        return transformedPoint
    }
    static void macroImageInputGUI() {
        // Create the dialog
        def dlg = createMacroImageInputDialog()

        // Define response validation
        dlg.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (!isValidInput(x1Field.getText()) || !isValidInput(y1Field.getText())) {
                    Dialogs.showWarningNotification("Invalid Input", "Please enter valid numeric values for coordinates.")
                    return null // Prevent dialog from closing
                }
            }
            return dialogButton
        })

        // Show the dialog and capture the response
        Optional<ButtonType> result = dlg.showAndWait()

        // Handling the response
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Retrieve values from text fields and checkbox
            String xCoordinate = x1Field.getText()
            String yCoordinate = y1Field.getText()
            String pixelSize = pixelSizeField.getText()
            boolean isSlideFlipped = slideFlippedCheckBox.isSelected()
            boolean arePixelsNonIsotropic = nonIsotropicCheckBox.isSelected()
            String groovyScriptPath = groovyScriptField.getText()
            def sampleLabel = sampleLabelField.getText()
            def virtualEnvPath = virtualEnvField.getText()
            def pythonScriptPath = pythonScriptField.getText()
            def projectsFolderPath = projectsFolderField.getText()

            // Check if data is all present
            if ([pixelSize, groovyScriptPath].any { it == null || it.isEmpty() }) {
                Dialogs.showWarningNotification("Warning!", "Insufficient data to send command to microscope!")
                return
            }
            //String imageName = QP.getCurrentImageName()

            Map scriptPaths = calculateScriptPaths(groovyScriptPath)
            String jsonTissueClassfierPathString = scriptPaths.jsonTissueClassfierPathString
            QuPathGUI qupathGUI = QPEx.getQuPath()

            //create a projectDetails map with four values that will be needed later, all related to project creation.
            Map projectDetails = createAndOpenQuPathProject(qupathGUI, projectsFolderPath, sampleLabel, preferences)
            Project currentQuPathProject = projectDetails.currentQuPathProject as Project
            String tempTileDirectory = projectDetails.tempTileDirectory


            String tissueDetectScript = UtilityFunctions.modifyTissueDetectScript(groovyScriptPath, pixelSize, jsonTissueClassfierPathString)
            //logger.info(tissueDetectScript)
            // Run the modified script
            QuPathGUI.getInstance().runScript(null, tissueDetectScript)
            //At this point the tissue should be outlined in an annotation

            boolean annotationStatusCheck = checkValidAnnotationsGUI()
            if (!annotationStatusCheck){
                return
            }

            def annotations = getAnnotationObjects().findAll{it.getPathClass().toString().equals("Tissue")}
            Double frameWidthMicrons = (preferences.frameWidth as Double) / (preferences.pixelSizeSource as Double) * (preferences.pixelSizeFirstScanType as Double)
            Double frameHeightMicrons = (preferences.frameHeight as Double) / (preferences.pixelSizeSource as Double) * (preferences.pixelSizeFirstScanType as Double)
            UtilityFunctions.performTilingAndSaveConfiguration(tempTileDirectory,
                                        projectDetails.scanTypeWithIndex.toString(),
                                        frameWidthMicrons,
                                        frameHeightMicrons,
                                        preferences.overlapPercent as Double,
                    null,
                              true,
                                        annotations)

            /////////////////////////////////////////
            //Dialog chain to validate stage location
            /////////////////////////////////////////
            //create a basic affine transformation, add the scaling information and a possible Y axis flip
            //Then create a dialog that asks the user to select a single detection tile
            AffineTransform transformation = TransformationFunctions.setupAffineTransformationAndValidationGUI(pixelSize as Double, isSlideFlipped, preferences, qupathGUI)
            //If user exited out of the dialog, the transformation should be null, and we do not want to continue.
            if (transformation == null){
                return
            }

            PathObject expectedTile = getSelectedObject()
            def detections = QP.getDetectionObjects()
            def topCenterTileXY = TransformationFunctions.getTopCenterTile(detections)
            def leftCenterTileXY = TransformationFunctions.getLeftCenterTile(detections)


            // Get the current stage coordinates to figure out the translation from the first alignment.
            List coordinatesQP = [expectedTile.getROI().getBoundsX(), expectedTile.getROI().getBoundsY()]
            if (!coordinatesQP){
                logger.error("Need coordinates.")
                return
            }
            logger.info("user adjusted position of tile at $coordinatesQP")
            List currentStageCoordinates_um = UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, null)
            logger.info("Obtained stage coordinates: $currentStageCoordinates_um")
            logger.info("QuPath coordinates for selected tile: $coordinatesQP")
            logger.info("affine transform before initial alignment: $transformation")
            transformation = TransformationFunctions.initialTransformation(transformation, coordinatesQP as List<String>, currentStageCoordinates_um as List<String>)
            logger.info("affine transform after initial alignment: $transformation")


            // Handle stage alignment for top center tile
            Map resultsTopCenter = handleStageAlignment(topCenterTileXY, qupathGUI, virtualEnvPath, pythonScriptPath, transformation)
            if (!resultsTopCenter.updatePosition) {
                logger.info("Window was closed, alignment cancelled.")
                return // Exit if position validation fails
            }
            transformation = resultsTopCenter.transformation as AffineTransform

            // Handle stage alignment for left center tile
            Map resultsLeftCenter = handleStageAlignment(leftCenterTileXY, qupathGUI, virtualEnvPath, pythonScriptPath, transformation)
            if (!resultsLeftCenter.updatePosition) {
                logger.info("Window was closed, alignment cancelled.")
                return // Exit if position validation fails
            }
            transformation = resultsLeftCenter.transformation as AffineTransform

            //The TileConfiguration.txt file created by the Groovy script is in QuPath pixel coordinates.
            //It must be transformed into stage coordinates in microns
            logger.info("export script path string $tempTileDirectory")
            def tileconfigFolders = TransformationFunctions.transformTileConfiguration(tempTileDirectory, transformation)
            for (folder in tileconfigFolders){
                logger.info("modified TileConfiguration at $folder")
            }

            // scanTypeWithIndex will be the name of the folder where the tiles will be saved to
            for (annotation in annotations) {

                List<String> args = [projectsFolderPath,
                                     sampleLabel,
                                     projectDetails.scanTypeWithIndex,
                                     annotation.getName(),
                ] as List<String>
                logger.info("Check input args for runPythonCommand")

                //TODO can we create non-blocking python code
                UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, args)


                String stitchedImagePathStr = UtilityFunctions.stitchImagesAndUpdateProject(projectsFolderPath,
                        sampleLabel, projectDetails.scanTypeWithIndex as String, annotation.getName(),
                        qupathGUI, currentQuPathProject, preferences.compression)
                logger.info("Stitching completed at $stitchedImagePathStr")
            }
            //Check if the tiles should be deleted from the collection folder
            if (preferences.tileHandling == "Delete")
                UtilityFunctions.deleteTilesAndFolder(tempTileDirectory)
            if (preferences.tileHandling == "Zip") {
                UtilityFunctions.zipTilesAndMove(tempTileDirectory)
                UtilityFunctions.deleteTilesAndFolder(tempTileDirectory)
            }


        }
    }

    /******************************************************
    *Starting point for creating a tiling grid from a bounding box using stage coordinates as inputs
    *********************************************************/
    static void boundingBoxInputGUI() {
        // Create the dialog
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setTitle("qp_scope")
        //dlg.setHeaderText("Enter details (LOOK MA! " + BasicStitchingExtension.class.getName() + "!):");

        // Set the content
        dlg.getDialogPane().setContent(createBoundingBoxInputGUI())

        // Add Okay and Cancel buttons
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

        // Show the dialog and capture the response
        def result = dlg.showAndWait()

        // Handling the response
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Retrieve values from text fields
            def sampleLabel = sampleLabelField.getText()
            def virtualEnvPath = virtualEnvField.getText()
            def pythonScriptPath = pythonScriptField.getText()
            def projectsFolderPath = projectsFolderField.getText()
            def x1 = x1Field.getText()
            def y1 = y1Field.getText()
            def x2 = x2Field.getText()
            def y2 = y2Field.getText()
            // Handle full bounding box input
            def boxString = scanBox.getText()
            //Boolean to check whether to proceed with running the microscope data collection
            boolean dataCheck = true
            def pixelSize = preferences.pixelSizeFirstScanType

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
                Map projectDetails = createAndOpenQuPathProject(qupathGUI, projectsFolderPath, sampleLabel, preferences)
                Project currentQuPathProject = projectDetails.currentQuPathProject as Project
                String tempTileDirectory = projectDetails.tempTileDirectory
                String scanTypeWithIndex = projectDetails.scanTypeWithIndex
                //Map scriptPaths = calculateScriptPaths(pythonScriptPath)

                //Specifically for the case where there is only a bounding box provided
                List<Double> boundingBoxValues = [x1, y1, x2, y2].collect { it.toDouble() }
                Double frameWidthMicrons = (preferences.frameWidth as Double) * (preferences.pixelSizeFirstScanType as Double)
                Double frameHeightMicrons = (preferences.frameHeight as Double)  * (preferences.pixelSizeFirstScanType as Double)
                UtilityFunctions.performTilingAndSaveConfiguration(tempTileDirectory, scanTypeWithIndex,
                        frameWidthMicrons,
                        frameHeightMicrons,
                        preferences.overlapPercent as Double,
                        boundingBoxValues,
                false)

                //Send the scanning command to the microscope

                List args = [projectsFolderPath,
                             sampleLabel,
                             scanTypeWithIndex,
                             "[$x1, $y1, $x2, $y2]"]
                //TODO can we create non-blocking python code
                UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, args)

                // Handle image stitching and update project
                String stitchedImagePathStr = UtilityFunctions.stitchImagesAndUpdateProject(projectsFolderPath,
                        sampleLabel, scanTypeWithIndex, "bounds", qupathGUI, currentQuPathProject,
                        preferences.compression)
                logger.info(stitchedImagePathStr)
//                qupathGUI.openImageEntry(currentQuPathProject.getImageList().find { image ->
//                    (new File(image.getImageName()).name == new File(stitchedImagePathStr).name)
//                })
                qupathGUI.refreshProject()
                //Check if the tiles should be deleted from the collection folder
                if (preferences.tileHandling == "Delete")
                    UtilityFunctions.deleteTilesAndFolder(tempTileDirectory)
                if (preferences.tileHandling == "Zip") {
                    UtilityFunctions.zipTilesAndMove(tempTileDirectory)
                    UtilityFunctions.deleteTilesAndFolder(tempTileDirectory)
                }
                //}
            }
        }
    }

    private static GridPane createBoundingBoxInputGUI() {
        GridPane pane = new GridPane()
        pane.setHgap(10)
        pane.setVgap(10)
        def row = 0

        // Add new component for Sample Label
        addToGrid(pane, new Label('Sample Label:'), sampleLabelField, row++)

        // Add existing components to the grid
        addToGrid(pane, new Label('X1:'), x1Field, row++)
        addToGrid(pane, new Label('Y1:'), y1Field, row++)
        addToGrid(pane, new Label('X2:'), x2Field, row++)
        addToGrid(pane, new Label('Y2:'), y2Field, row++)
        addToGrid(pane, new Label('Full bounding box:'), scanBox, row++)

        // Add components for Python environment and script path
        addToGrid(pane, new Label('Python Virtual Env Location:'), virtualEnvField, row++)
        addToGrid(pane, new Label('PycroManager .py path:'), pythonScriptField, row++)
        addToGrid(pane, new Label('Projects parent folder:'), projectsFolderField, row++)

        return pane
    }


    static void secondModalityGUI() {
        //TODO check if in a project?
        def logger = LoggerFactory.getLogger(QuPathGUI.class)
        // Create the dialog
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setTitle("Collect image data from an annotated subset of your current image.")
        dlg.setHeaderText("Create annotations within your image, then click Okay to proceed with a second collection within those areas.")

        // Set the content
        dlg.getDialogPane().setContent(createSecondModalityGUI())

        // Add Okay and Cancel buttons
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

        // Show the dialog and capture the response
        def result = dlg.showAndWait()

        // Handling the response
        logger.info("Starting processing GUI output")
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Retrieve values from text fields
            def sampleLabel = sampleLabelField.getText()
            def classFilter = classFilterField.getText().split(',').collect { it.trim() }
            def virtualEnvPath = virtualEnvField.getText()
            def pythonScriptPath = pythonScriptField.getText()
            def projectsFolderPath = projectsFolderField.getText()

            //SETUP: collect variables
            QuPathGUI qupathGUI = QPEx.getQuPath()
            String scanTypeWithIndex = MinorFunctions.getUniqueFolderName(projectsFolderPath + File.separator + sampleLabel + File.separator + preferences.secondScanType)
            String tempTileDirectory = projectsFolderPath + File.separator + sampleLabel + File.separator + scanTypeWithIndex
            Project<BufferedImage> currentQuPathProject = getProject()
            //Boolean to check whether to proceed with running the microscope data collection
            logger.info("getting annotation objects")
            def annotations = getAnnotationObjects()
            annotations = annotations.findAll{classFilter.contains(it.getPathClass().toString())}

            // Check if we have sufficient information to proceed
            if (annotations.isEmpty() || [sampleLabel, virtualEnvPath, pythonScriptPath].any { it == null || it.isEmpty() }) {
                Dialogs.showWarningNotification("Warning!", "Insufficient data to send command to microscope!")
                return
            }
            //Convert the camera frame width/height into pixels in the image we are working on.
            Double frameWidthQPpixels = (preferences.frameWidth as Double) / (preferences.pixelSizeSource as Double) * (preferences.pixelSizeFirstScanType as Double)
            Double frameHeightQPpixels = (preferences.frameHeight as Double) / (preferences.pixelSizeSource as Double) * (preferences.pixelSizeFirstScanType as Double)
            UtilityFunctions.performTilingAndSaveConfiguration(tempTileDirectory,
                    scanTypeWithIndex,
                    frameWidthQPpixels,
                    frameHeightQPpixels,
                    preferences.overlapPercent as Double,
                    null,
                    true,
                    annotations)

            logger.info("Scan type with index: " + scanTypeWithIndex)
            logger.info(tempTileDirectory)

// A semaphore to control the stitching process
            Semaphore stitchingSemaphore = new Semaphore(1)


            for (annotation in annotations) {
                String annotationName = annotation.getName()
                List args = [projectsFolderPath,
                             sampleLabel,
                             scanTypeWithIndex,
                             annotationName]
                //TODO how can we distinguish between a hung python run and one that is taking a long time? - possibly check for new files in target folder?
                //TODO Need to check if stitching is successful, provide error

                //Progress bar that updates by checking target folder for new images?
                UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, args)
                //UtilityFunctions.runPythonCommand(virtualEnvPath,  "C:\\ImageAnalysis\\python\\py_dummydoc.py", args)
                logger.info("Finished Python Command for $annotationName")
                // Start a new thread for stitching
                Thread.start {
                    try {
                        // Acquire a permit before starting stitching
                        stitchingSemaphore.acquire()

                        logger.info("Begin stitching")
                        String stitchedImagePathStr = UtilityFunctions.stitchImagesAndUpdateProject(projectsFolderPath,
                                sampleLabel, scanTypeWithIndex as String, annotationName,
                                qupathGUI, currentQuPathProject, preferences.compression)
                        logger.info(stitchedImagePathStr)


                    } catch (InterruptedException e) {
                        logger.error("Stitching thread interrupted", e)
                    } finally {
                        // Release the semaphore for the next stitching process
                        stitchingSemaphore.release()
                    }
                }

            }
            // Post-stitching tasks like deleting or zipping tiles
            //Check if the tiles should be deleted from the collection folder
            if (preferences.tileHandling == "Delete")
                UtilityFunctions.deleteTilesAndFolder(tempTileDirectory)
            if (preferences.tileHandling == "Zip") {
                UtilityFunctions.zipTilesAndMove(tempTileDirectory)
                UtilityFunctions.deleteTilesAndFolder(tempTileDirectory)
            }
        }
    }


    private static void addToGrid(GridPane pane, Node label, Node control, int rowIndex) {
        pane.add(label, 0, rowIndex)
        pane.add(control, 1, rowIndex)
    }
    // Overloaded addToGrid method for a single Node
    // TODO fix hardcoding of 2 and 1
    private static void addToGrid(GridPane pane, Node node, int rowIndex) {
        pane.add(node, 0, rowIndex, 2, 1) // The node spans 2 columns
    }


    private static Dialog<ButtonType> createMacroImageInputDialog() {
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setTitle("Macro View Configuration")
        dlg.setHeaderText("Configure settings for macro view.")
        dlg.getDialogPane().setContent(createMacroImageInputGUI())
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)
        return dlg
    }

    // Helper method to check if input is numeric
    private static boolean isValidInput(String input) {
        return input.matches("\\d*")
    }


    private static GridPane createMacroImageInputGUI() {
        GridPane pane = new GridPane()
        pane.setHgap(10)
        pane.setVgap(10)
        def row = 0

        // Add new components for the checkbox and Groovy script path
        addToGrid(pane, new Label('Sample Label:'), sampleLabelField, row++)
        // Add components for Python environment and script path
        addToGrid(pane, new Label('Python Virtual Env folder:'), virtualEnvField, row++)
        addToGrid(pane, new Label('PycroManager control file:'), pythonScriptField, row++)
        addToGrid(pane, new Label('Projects path:'), projectsFolderField, row++)

        addToGrid(pane, new Label('Slide flipped:'), slideFlippedCheckBox, row++)
        addToGrid(pane, new Label('Tissue detection script:'), groovyScriptField, row++)
        // Add new components for pixel size and non-isotropic pixels checkbox on the same line
        HBox pixelSizeBox = new HBox(10)
        pixelSizeBox.getChildren().addAll(new Label('Pixel Size XY um:'), pixelSizeField, nonIsotropicCheckBox)
        addToGrid(pane, pixelSizeBox, row++)
        // Add new components for "Upper left XY coordinate"
        //Label upperLeftLabel = new Label("Upper left XY coordinate")
        //pane.add(upperLeftLabel, 0, row); // Span multiple columns if needed

        //addToGrid(pane, new Label('X coordinate:'), x1Field, ++row);
        //addToGrid(pane, new Label('Y coordinate:'), y1Field, ++row);


        return pane
    }

    static boolean stageToQuPathAlignmentGUI1() {
        Dialog<ButtonType> dlg = new Dialog<>()
        dlg.initModality(Modality.NONE)
        dlg.setTitle("Identify Location")
        dlg.setHeaderText("Select one tile (a detection) and match the Live View in uManager to the location of that tile, as closely as possible.\n This will be used for matching QuPath's coordinate system to the microscope stage coordinate system, so be as careful as you can!")
        // Add buttons to the dialog
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

        Optional<ButtonType> result
        boolean validTile = false

        while (!validTile) {
            // Show the dialog and wait for the user response
            result = dlg.showAndWait()

            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Check for expected rectangle
                List selectedObjects = QP.getSelectedObjects().stream()
                        .filter(object -> object.isDetection() && object.getROI() instanceof qupath.lib.roi.RectangleROI)
                        .collect(Collectors.toList())

                if (selectedObjects.size() != 1) {
                    // Use UtilityFunctions to show a warning
                    MinorFunctions.showAlertDialog("There needs to be exactly one tile selected.")
                } else {
                    validTile = true
                }
            } else {
                // User cancelled or closed the dialog
                return false
            }
        }
        return true
    }


    static boolean checkValidAnnotationsGUI() {
        Dialog<ButtonType> dlg = new Dialog<>()
        dlg.initModality(Modality.NONE)
        dlg.setTitle("Validate annotation boundaries")
        dlg.setHeaderText("Check the existing annotations to make sure what you want to image exists within an annotation. \n Delete, edit, or manually create any changes you would want at this stage, then click OK.")
        // Add buttons to the dialog
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)
        Optional<ButtonType> result = dlg.showAndWait()
        return result.isPresent() && result.get() == ButtonType.OK

    }

    static stageToQuPathAlignmentGUI2() {
        List<String> choices = Arrays.asList("Yes", "Use adjusted position")
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Yes", choices)
        dialog.initModality(Modality.NONE)
        dialog.setTitle("Position Confirmation")
        dialog.setHeaderText("Is the current position accurate? Compare with the uManager live view!\n The first time this dialog shows up, it should select the center of the top row! \n The second time, it should select the center of the left-most column!")

        Optional<String> result = dialog.showAndWait()
        if (result.isPresent()) {

            return result.get()

        }

        // If no choice is made (e.g., dialog is closed), you can decide to return false or handle it differently
        return false
    }

    /**
     * Handles the process of selecting a tile, transforming its coordinates, moving the stage,
     * validating the new stage position, and updating the transformation.
     *
     * @param tileXY The tile coordinates and object.
     * @param qupathGUI The QuPath GUI instance.
     * @param virtualEnvPath
     The virtual environment path for Python commands.

     @param pythonScriptPath The Python script path.

     @param transformation The current AffineTransform.

     @return A boolean indicating if the position was validated successfully and the updated transformation.
     */

    private static Map<String, Object> handleStageAlignment(PathObject tileXY, QuPathGUI qupathGUI,
                                                            String virtualEnvPath, String pythonScriptPath,
                                                            AffineTransform transformation) {
        QP.selectObjects(tileXY)
        // Transform the QuPath coordinates into stage coordinates
        def QPPixelCoordinates = [tileXY.getROI().getCentroidX(), tileXY.getROI().getCentroidY()]
        List expectedStageXYPositionMicrons = TransformationFunctions.QPtoMicroscopeCoordinates(QPPixelCoordinates, transformation)
        logger.info("QuPath pixel coordinates: $QPPixelCoordinates")
        logger.info("Transformed into stage coordinates: $expectedStageXYPositionMicrons")
        // Move the stage to the new coordinates
        UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, expectedStageXYPositionMicrons)
        qupathGUI.getViewer().setCenterPixelLocation(tileXY.getROI().getCentroidX(), tileXY.getROI().getCentroidY())

        // Validate the position that was moved to or update with an adjusted position
        def updatePosition = stageToQuPathAlignmentGUI2()
        if (updatePosition.equals("Use adjusted position")) {
            // Get access to current stage coordinates and update transformation
            List currentStageCoordinates_um = UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, null)
            transformation = TransformationFunctions.initialTransformation(transformation, QPPixelCoordinates as List<String>, currentStageCoordinates_um)
        }

        // Prepare the results to be returned
        Map<String, Object> results = [
                'updatePosition': updatePosition,
                'transformation': transformation
        ]

        return results
    }

    //Create the second interface window for performing higher resolution or alternate modality scans
    private static GridPane createSecondModalityGUI() {
        GridPane pane = new GridPane()
        pane.setHgap(10)
        pane.setVgap(10)
        def row = 0

        // Add new component for Sample Label
        addToGrid(pane, new Label('Sample Label:'), sampleLabelField, row++)
        addToGrid(pane, new Label('Annotation classes to image:'), classFilterField, row++)


        // Add components for Python environment and script path
        addToGrid(pane, new Label('Python Virtual Env Location:'), virtualEnvField, row++)
        addToGrid(pane, new Label('PycroManager .py file path:'), pythonScriptField, row++)
        addToGrid(pane, new Label('Projects path:'), projectsFolderField, row++)
        // Listener for the checkbox

        return pane
    }

    private static Map<String, String> calculateScriptPaths(String groovyScriptPath) {
        Path groovyScriptDirectory = Paths.get(groovyScriptPath).getParent()
        groovyScriptDirectory = groovyScriptDirectory.resolveSibling("groovyScripts")

        Path jsonTissueClassfierPath = groovyScriptDirectory.resolve("Tissue-lowres.json")
        Path exportScriptPath = groovyScriptDirectory.resolve("save4xMacroTiling.groovy")

        return [
                jsonTissueClassfierPathString: jsonTissueClassfierPath.toString().replace("\\", "/"),
                exportScriptPathString: exportScriptPath.toString().replace("\\", "/")
        ]
    }

    /**
     * Creates a new QuPath project, adds the current image to it, and opens the project.
     *
     * @param projectsFolderPath The path where the project will be located.
     * @param sampleLabel The label for the sample.
     * @param preferences User preferences that include settings like scan type.
     * @return A map containing the
     * created project
     * temporary tile directory String
     * matchingImage ProjectImageEntry
     * scanTypeWithIndex string
     */
    static Map<String, Object> createAndOpenQuPathProject(QuPathGUI qupathGUI, String projectsFolderPath, String sampleLabel, Map<String,String> preferences) {
        Project currentQuPathProject = UtilityFunctions.createProjectFolder(projectsFolderPath, sampleLabel, preferences.firstScanType)

        String scanTypeWithIndex = MinorFunctions.getUniqueFolderName(projectsFolderPath + File.separator + sampleLabel + File.separator + preferences.firstScanType)
        String tempTileDirectory = projectsFolderPath + File.separator + sampleLabel + File.separator + scanTypeWithIndex

        String macroImagePath = null
        ProjectImageEntry matchingImage = null

        if (QP.getCurrentImageData() != null) {
            logger.info("current image exists")
            String serverPath = QP.getCurrentImageData().getServerPath()
            logger.info(serverPath)
            macroImagePath = MinorFunctions.extractFilePath(serverPath)
            logger.info(macroImagePath)
            if (macroImagePath != null) {
                logger.info("Extracted file path: $macroImagePath")
                UtilityFunctions.addImageToProject(new File(macroImagePath), currentQuPathProject)
                logger.info("image added to project")
                qupathGUI.setProject(currentQuPathProject)
                matchingImage = currentQuPathProject.getImageList().find { image ->
                    new File(image.getImageName()).name == new File(macroImagePath).name
                }
                logger.info("open image ")
                qupathGUI.openImageEntry(matchingImage)
                qupathGUI.refreshProject()
            } else {
                logger.info("File path could not be extracted.")
            }
        } else {
            logger.info("No current image data available.")
        }



        return [
                'matchingImage': matchingImage,
                'scanTypeWithIndex': scanTypeWithIndex,
                'currentQuPathProject': currentQuPathProject,
                'tempTileDirectory': tempTileDirectory
        ]
    }



}
