package qupath.ext.qp_scope.functions

import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.stage.Modality
import org.slf4j.LoggerFactory
import qupath.ext.basicstitching.stitching.stitchingImplementations
import qupath.ext.qp_scope.utilities.utilityFunctions
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.gui.scripting.QPEx
import qupath.lib.projects.Project
import qupath.lib.scripting.QP

import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

import static qupath.lib.scripting.QP.getAnnotationObjects
import static qupath.lib.scripting.QP.project

//Thoughts:
//Have collapsible sections to a larger dialog box?
//        Alternatively, individual dialogs for each step, but have menu options for each as well. The menu options for steps not yet reached would need to be greyed out.

class QP_scope_GUI {

    // Existing text fields
    static TextField x1Field = new TextField("")
    static TextField y1Field = new TextField("")
    static TextField x2Field = new TextField("")
    static TextField y2Field = new TextField("")
    static TextField scanBox = new TextField("20,25,30,35")
    static preferences = utilityFunctions.getPreferences()
    static CheckBox useAnnotationsCheckBox = new CheckBox("Use annotations")

    // New text fields for Python environment, script path, and sample label
    static TextField virtualEnvField = new TextField(preferences.environment)
    static TextField pythonScriptField = new TextField(preferences.pycromanager)
    static TextField projectsFolderField = new TextField(preferences.projects)
    static TextField sampleLabelField = new TextField("First_Test")  // New field for sample label
    // GUI3
    static CheckBox slideFlippedCheckBox = new CheckBox("Slide is flipped")
    static TextField groovyScriptField = new TextField("C:\\ImageAnalysis\\QPExtensionTest\\qp_scope\\src\\main\\groovyScripts/DetectTissue.groovy")
    // Default empty
    static TextField pixelSizeField = new TextField("7.2") // Default empty
    static CheckBox nonIsotropicCheckBox = new CheckBox("Non-isotropic pixels")

    static void createGUI1() {
        // Create the dialog
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setTitle("qp_scope")
        //dlg.setHeaderText("Enter details (LOOK MA! " + BasicStitchingExtension.class.getName() + "!):");

        // Set the content
        dlg.getDialogPane().setContent(createContent())

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
            def annotationJsonFileLocation = null
            // Handle full bounding box input
            def boxString = scanBox.getText()
            //Boolean to check whether to proceed with running the microscope data collection
            boolean dataCheck = true
            def annotations = QP.getAnnotationObjects()
            // Check if using annotations

            //TODO REPLACE ALL OF THIS WITH TILING
            if (useAnnotationsCheckBox.isSelected()) {

                // Check if annotations are present
                if (annotations.isEmpty() || [sampleLabel, virtualEnvPath, pythonScriptPath].any { it == null || it.isEmpty() }) {
                    Dialogs.showWarningNotification("Warning!", "Insufficient data to send command to microscope!")
                    dataCheck = false
                    return
                }
                annotationJsonFileLocation = utilityFunctions.createAnnotationJson(projectsFolderPath, sampleLabel, preferences.firstScanType)
            } else {
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
            }

            // Check if any value is empty
            if (dataCheck) {
                Project currentQuPathProject = utilityFunctions.createProjectFolder(projectsFolderPath, sampleLabel, preferences.firstScanType)
                def scanTypeWithIndex = utilityFunctions.getUniqueFolderName(projectsFolderPath + File.separator + sampleLabel + File.separator + preferences.firstScanType)
                def tempTileDirectory = projectsFolderPath + File.separator + sampleLabel + File.separator + scanTypeWithIndex
                def logger = LoggerFactory.getLogger(QuPathGUI.class)
                logger.info(tempTileDirectory)
                //Reduce the number of sent args
                def boundingBox = "{$x1}, {$y1}, {$x2}, {$y2}"
                // scanTypeWithIndex will be the name of the folder where the tiles will be saved to

                List args = [pythonScriptPath,
                             projectsFolderPath,
                             sampleLabel,
                             scanTypeWithIndex,
                             annotationJsonFileLocation,
                             boundingBox]
                //TODO can we create non-blocking python code
                utilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, args)


                String stitchedImageOutputFolder = projectsFolderPath + File.separator + sampleLabel + File.separator + "SlideImages"
                //TODO Need to check if stitching is successful, provide error
                //stitchingImplementations.stitchCore(stitchingType, folderPath, compressionType, pixelSize, downsample, matchingString)
                //TODO add output folder to stitchCore
                String stitchedImagePathStr = stitchingImplementations.stitchCore("Coordinates in TileConfiguration.txt file", projectsFolderPath + File.separator + sampleLabel, stitchedImageOutputFolder, "J2K_LOSSY", 0, 1, scanTypeWithIndex)


                //utilityFunctions.showAlertDialog("Wait and complete stitching in other version of QuPath")

                //String stitchedImagePathStr = stitchedImageOutputFolder + File.separator + preferences.firstScanType + sampleLabel + ".ome.tif"
                File stitchedImagePath = new File(stitchedImagePathStr)
                utilityFunctions.addImageToProject(stitchedImagePath, currentQuPathProject)

                //open the newly created project
                //https://qupath.github.io/javadoc/docs/qupath/lib/gui/QuPathGUI.html#setProject(qupath.lib.projects.Project)
                def qupathGUI = QPEx.getQuPath()

                qupathGUI.setProject(currentQuPathProject)
                //Find the existing images - there should only be one since the project was just created
                def matchingImage = currentQuPathProject.getImageList().find { image ->
                    (new File(image.getImageName()).name == new File(stitchedImagePathStr).name)
                }

                //Open the first image
                //https://qupath.github.io/javadoc/docs/qupath/lib/gui/QuPathGUI.html#openImageEntry(qupath.lib.projects.ProjectImageEntry)
                qupathGUI.openImageEntry(matchingImage)
                qupathGUI.refreshProject()
                //Check if the tiles should be deleted from the collection folder
                if (preferences.tileHandling == "Delete")
                    utilityFunctions.deleteTilesAndFolder(tempTileDirectory)
                if (preferences.tileHandling == "Zip") {
                    utilityFunctions.zipTilesAndMove(tempTileDirectory)
                    utilityFunctions.deleteTilesAndFolder(tempTileDirectory)
                }
                //}
            }
        }
    }

    private static GridPane createContent() {
        GridPane pane = new GridPane()
        pane.setHgap(10)
        pane.setVgap(10)
        def row = 0

        // Add new component for Sample Label
        addToGrid(pane, new Label('Sample Label:'), sampleLabelField, row++)
        addToGrid(pane, new Label('Use annotations in current image?'), useAnnotationsCheckBox, row++)

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
        // Listener for the checkbox
        useAnnotationsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            x1Field.setDisable(newValue)
            y1Field.setDisable(newValue)
            x2Field.setDisable(newValue)
            y2Field.setDisable(newValue)
            scanBox.setDisable(newValue)
            // You can also disable other related fields if necessary
        })
        return pane
    }

    private static void addToGrid(GridPane pane, Node label, Node control, int rowIndex) {
        pane.add(label, 0, rowIndex)
        pane.add(control, 1, rowIndex)
    }
    // Overloaded addToGrid method for a single Node
    // TODO fix hardcoding of 2 and 1
    private static void addToGrid(GridPane pane, Node node, int rowIndex) {
        pane.add(node, 0, rowIndex, 2, 1); // The node spans 2 columns
    }

    static void createGUI2() {
        //TODO check if in a project?
        def logger = LoggerFactory.getLogger(QuPathGUI.class)
        // Create the dialog
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setTitle("Collect image data from an annotated subset of your current image.")
        dlg.setHeaderText("Create annotations within your image, then click Okay to proceed with a second collection within those areas.");

        // Set the content
        dlg.getDialogPane().setContent(createContent2())

        // Add Okay and Cancel buttons
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

        // Show the dialog and capture the response
        def result = dlg.showAndWait()

        // Handling the response
        logger.info("Starting processing GUI output")
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Retrieve values from text fields
            def sampleLabel = sampleLabelField.getText()
            def virtualEnvPath = virtualEnvField.getText()
            def pythonScriptPath = pythonScriptField.getText()
            def projectsFolderPath = projectsFolderField.getText()

            def annotationJsonFileLocation = null

            //Boolean to check whether to proceed with running the microscope data collection
            logger.info("getting annotation objects")
            def annotations = QP.getAnnotationObjects()

            // Check if annotations are present
            if (annotations.isEmpty() || [sampleLabel, virtualEnvPath, pythonScriptPath].any { it == null || it.isEmpty() }) {
                Dialogs.showWarningNotification("Warning!", "Insufficient data to send command to microscope!")

                return
            }


            def scanTypeWithIndex = utilityFunctions.getUniqueFolderName(projectsFolderPath + File.separator + sampleLabel + File.separator + preferences.secondScanType)
            def tempTileDirectory = projectsFolderPath + File.separator + sampleLabel + File.separator + scanTypeWithIndex
            logger.info("Scan type with index: " + scanTypeWithIndex)
            logger.info(tempTileDirectory)
            logger.info("Creating json")
            annotationJsonFileLocation = utilityFunctions.createAnnotationJson(projectsFolderPath, sampleLabel, scanTypeWithIndex)

            List args = [pythonScriptPath, projectsFolderPath, sampleLabel, scanTypeWithIndex, annotationJsonFileLocation]
            //TODO how can we distinguish between a hung python run and one that is taking a long time? - possibly check for new files in target folder?
            utilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, args)
            //utilityFunctions.runPythonCommand(virtualEnvPath,  "C:\\ImageAnalysis\\python\\py_dummydoc.py", args)
            logger.info("Finished Python Command")
            String stitchedImageOutputFolder = projectsFolderPath + File.separator + sampleLabel + File.separator + "SlideImages"
            //TODO Need to check if stitching is successful, provide error
            //TODO get pixel size from somewhere???

            //stitchingImplementations.stitchCore(stitchingType, folderPath, compressionType, pixelSize, downsample, matchingString)
            logger.info("Begin stitching")
            String stitchedImagePathStr = stitchingImplementations.stitchCore("Coordinates in TileConfiguration.txt file", projectsFolderPath + File.separator + sampleLabel, stitchedImageOutputFolder, "J2K_LOSSY", 0, 1, scanTypeWithIndex)
            logger.info("Get project")
            Project<BufferedImage> currentQuPathProject = getProject()

            //utilityFunctions.showAlertDialog("Wait and complete stitching in other version of QuPath")

            //String stitchedImagePathStr = stitchedImageOutputFolder + File.separator + preferences.secondScanType + sampleLabel + ".ome.tif"
            File stitchedImagePath = new File(stitchedImagePathStr)
            utilityFunctions.addImageToProject(stitchedImagePath, currentQuPathProject)

            //open the newly created project
            //https://qupath.github.io/javadoc/docs/qupath/lib/gui/QuPathGUI.html#setProject(qupath.lib.projects.Project)
            def qupathGUI = QPEx.getQuPath()

            //qupathGUI.setProject(currentQuPathProject)
            //Find the existing images - there should only be one since the project was just created
            def matchingImage = currentQuPathProject.getImageList().find { image ->
                (new File(image.getImageName()).name == new File(stitchedImagePathStr).name)
            }
            qupathGUI.refreshProject()
            //Open the first image
            //https://qupath.github.io/javadoc/docs/qupath/lib/gui/QuPathGUI.html#openImageEntry(qupath.lib.projects.ProjectImageEntry)
            qupathGUI.openImageEntry(matchingImage)

            //Check if the tiles should be deleted from the collection folder
            //Check if the tiles should be deleted from the collection folder
            if (preferences.tileHandling == "Delete")
                utilityFunctions.deleteTilesAndFolder(tempTileDirectory)
            if (preferences.tileHandling == "Zip") {
                utilityFunctions.zipTilesAndMove(tempTileDirectory)
                utilityFunctions.deleteTilesAndFolder(tempTileDirectory)
            }

        }
    }


    //Create the second interface window for performing higher resolution or alternate modality scans
    private static GridPane createContent2() {
        GridPane pane = new GridPane()
        pane.setHgap(10)
        pane.setVgap(10)
        def row = 0

        // Add new component for Sample Label
        addToGrid(pane, new Label('Sample Label:'), sampleLabelField, row++)


        // Add components for Python environment and script path
        addToGrid(pane, new Label('Python Virtual Env Location:'), virtualEnvField, row++)
        addToGrid(pane, new Label('PycroManager .py file path:'), pythonScriptField, row++)
        addToGrid(pane, new Label('Projects path:'), projectsFolderField, row++)
        // Listener for the checkbox

        return pane
    }

    static void createGUI3() {
        // Create the dialog
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setTitle("Macro View Configuration")
        dlg.setHeaderText("Configure settings for macro view.")

        // Set the content
        dlg.getDialogPane().setContent(createContent3())

        // Add Okay and Cancel buttons
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

        // Define response validation
        dlg.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (!isValidInput(x1Field.getText()) || !isValidInput(y1Field.getText())) {
                    Dialogs.showWarningNotification("Invalid Input", "Please enter valid numeric values for coordinates.")
                    return null; // Prevent dialog from closing
                }
            }
            return dialogButton;
        });

        // Show the dialog and capture the response
        Optional<ButtonType> result = dlg.showAndWait();
        def logger = LoggerFactory.getLogger(QuPathGUI.class)
        // Handling the response
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Retrieve values from text fields and checkbox
            String xCoordinate = x1Field.getText();
            String yCoordinate = y1Field.getText();
            String pixelSize = pixelSizeField.getText();
            boolean isSlideFlipped = slideFlippedCheckBox.isSelected();
            boolean arePixelsNonIsotropic = nonIsotropicCheckBox.isSelected();
            String groovyScriptPath = groovyScriptField.getText();
            def sampleLabel = sampleLabelField.getText()
            def virtualEnvPath = virtualEnvField.getText()
            def pythonScriptPath = pythonScriptField.getText()
            def projectsFolderPath = projectsFolderField.getText()

            // Check if data is all present
            if ([pixelSize, groovyScriptPath].any { it == null || it.isEmpty() }) {
                Dialogs.showWarningNotification("Warning!", "Insufficient data to send command to microscope!")
                return
            }
            String imageName = QP.getCurrentImageName();

            // Determine the pixel size based on imageName
            if (imageName.contains("3600")) {
                pixelSize = "2.0";
            } else if (imageName.contains("7200")) {
                pixelSize = "1.0";
            }

            // Expect the classifier file path to be in a specific location
            // get the classifier from the groovyScripts folder, which should be "next to" the pythonScripts folder
            Path groovyScriptDirectory = Paths.get(pythonScriptPath).getParent();
            groovyScriptDirectory = groovyScriptDirectory.resolveSibling("groovyScripts")

            // Combine the directory with the new filename
            Path jsonFilePath = groovyScriptDirectory.resolve("Tissue-lowres.json");
            Path exportScriptPath = groovyScriptDirectory.resolve("save4xMacroTiling.groovy")
            // Convert Path back to String and fix slashes to not be escape chars
            String jsonFilePathString = jsonFilePath.toString().replace("\\", "/");
            String exportScriptPathString = exportScriptPath.toString().replace("\\", "/");

            //Create the QuPath project
            Project currentQuPathProject = utilityFunctions.createProjectFolder(projectsFolderPath, sampleLabel, preferences.firstScanType)
            def scanTypeWithIndex = utilityFunctions.getUniqueFolderName(projectsFolderPath + File.separator + sampleLabel + File.separator + preferences.firstScanType)
            def tempTileDirectory = projectsFolderPath + File.separator + sampleLabel + File.separator + scanTypeWithIndex


            //Get the current image open in QuPath and add it to the project
            def serverPath = QP.getCurrentImageData().getServerPath()

            String macroImagePath = utilityFunctions.extractFilePath(serverPath);

            if (macroImagePath != null) {
                logger.info("Extracted file path: " + macroImagePath);
            } else {
                logger.info("File path could not be extracted.");
            }

            //open the newly created project
            //https://qupath.github.io/javadoc/docs/qupath/lib/gui/QuPathGUI.html#setProject(qupath.lib.projects.Project)
            def qupathGUI = QPEx.getQuPath()

            utilityFunctions.addImageToProject(new File(macroImagePath), currentQuPathProject)
            qupathGUI.setProject(currentQuPathProject)
            //Find the existing images - there should only be one since the project was just created
            def matchingImage = currentQuPathProject.getImageList().find { image ->
                (new File(image.getImageName()).name == new File(macroImagePath).name)
            }

            //Open the first image
            //https://qupath.github.io/javadoc/docs/qupath/lib/gui/QuPathGUI.html#openImageEntry(qupath.lib.projects.ProjectImageEntry)
            qupathGUI.openImageEntry(matchingImage)

            qupathGUI.refreshProject()

            String tissueDetectScript = utilityFunctions.modifyTissueDetectScript(groovyScriptPath, pixelSize, jsonFilePathString)
            //logger.info(tissueDetectScript)
            // Run the modified script
            QuPathGUI.getInstance().runScript(null, tissueDetectScript);
            //At this point the tissue should be outlined in an annotation

            //Create an export tile locations
            String tilesCSVdirectory = projectsFolderPath + File.separator + sampleLabel + File.separator + "tiles_csv";
            String exportScript = utilityFunctions.modifyCSVExportScript(exportScriptPathString, pixelSize, tilesCSVdirectory)
            logger.info(exportScript)
            logger.info(tilesCSVdirectory)
            logger.info(exportScriptPathString)
            QuPathGUI.getInstance().runScript(null, exportScript);

            //////////////////////////////////////
            //Dialog chain to validate stage location
            //////////////////////////////////////
            // the transformation consists of an X-shift in stage microns, a Y-shift in stage microns, and a pixelSize
            def transformation = [0, 0, pixelSize as double]
            boolean gui4Success = createGUI4();
            if (!gui4Success) {
                // User cancelled GUI4, so end GUI3 and do not proceed
                return;
            }
            // Execute Python command to move stage
            def detections = QP.getDetectionObjects()
            def topCenterTileXY = utilityFunctions.getTopCenterTile(detections)
            QP.selectObjects(topCenterTileXY[2])
            List args = [topCenterTileXY[0], topCenterTileXY[1]]
            QuPathGUI.getInstance().getViewer().setCenterPixelLocation(topCenterTileXY[2].getROI().getCentroidX(), topCenterTileXY[2].getROI().getCentroidY())
            //TODO run python script to move the stage to the middle X value of the lowest Y value
            utilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, args)
            //Validate the position that was moved to or update with an adjusted position
            boolean updatePosition = createGUI5()

            if (updatePosition) {
                //TODO get access to current stage coordinates
                List currentStageCoordinates_um = utilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, null)
                transformation = utilityFunctions.updateTransformation(transformation, currentStageCoordinates_um, args)
            }

            def leftCenterTileXY = utilityFunctions.getLeftCenterTile(detections)
            QP.selectObjects(leftCenterTileXY[2])
            args = [leftCenterTileXY[0], leftCenterTileXY[1]]
            QuPathGUI.getInstance().getViewer().setCenterPixelLocation(leftCenterTileXY[2].getROI().getCentroidX(), leftCenterTileXY[2].getROI().getCentroidY())
            //TODO run python script to move the stage to the a tile position with the lowest X value, mid Y value
            utilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, args)
            //Once again, validate the position or update
            updatePosition = createGUI5()
            if (updatePosition) {
                //TODO get access to current stage coordinates

                List currentStageCoordinates_um = utilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, null)
                transformation = utilityFunctions.updateTransformation(transformation, currentStageCoordinates_um, args)
            }

            // Additional code for annotations
            def annotations = getAnnotationObjects().findAll { it.getPathClass() == QP.getPathClass('Tissue') }
            if (annotations.size() != 1) {
                Dialogs.showWarningNotification("Error!", "Can only handle 1 annotation at the moment!");
                return;
            }

            def x1 = annotations[0].getROI().getBoundsX()
            def y1 = annotations[0].getROI().getBoundsY()
            def x2 = annotations[0].getROI().getBoundsWidth()
            def y2 = annotations[0].getROI().getBoundsHeight()
            // TODO Check if any value is empty

            //Send the QuPath pixel coordinates for the bounding box along with the pixel size and upper left coordinates of the tissue
            def boundingBox = utilityFunctions.transformBoundingBox(x1, y1, x2, y2, pixelSize, xCoordinate, yCoordinate, isSlideFlipped)


            logger.info(tilesCSVdirectory)


            // scanTypeWithIndex will be the name of the folder where the tiles will be saved to

            args = [pythonScriptPath,
                    projectsFolderPath,
                    sampleLabel,
                    scanTypeWithIndex,
                    tilesCSVdirectory, //no annotation JSON file location
                    boundingBox]
            //TODO can we create non-blocking python code
            utilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, args)


            String stitchedImageOutputFolder = projectsFolderPath + File.separator + sampleLabel + File.separator + "SlideImages"
            //TODO Need to check if stitching is successful, provide error
            //stitchingImplementations.stitchCore(stitchingType, folderPath, compressionType, pixelSize, downsample, matchingString)
            //TODO add output folder to stitchCore
            String stitchedImagePathStr = stitchingImplementations.stitchCore("Coordinates in TileConfiguration.txt file", projectsFolderPath + File.separator + sampleLabel, stitchedImageOutputFolder, "J2K_LOSSY", 0, 1, scanTypeWithIndex)


            //utilityFunctions.showAlertDialog("Wait and complete stitching in other version of QuPath")

            //String stitchedImagePathStr = stitchedImageOutputFolder + File.separator + preferences.firstScanType + sampleLabel + ".ome.tif"
            File stitchedImagePath = new File(stitchedImagePathStr)
            utilityFunctions.addImageToProject(stitchedImagePath, currentQuPathProject)


            qupathGUI.setProject(currentQuPathProject)
            //Find the existing images - there should only be one since the project was just created
            matchingImage = currentQuPathProject.getImageList().find { image ->
                (new File(image.getImageName()).name == new File(stitchedImagePathStr).name)
            }

            //Open the first image
            //https://qupath.github.io/javadoc/docs/qupath/lib/gui/QuPathGUI.html#openImageEntry(qupath.lib.projects.ProjectImageEntry)
            qupathGUI.openImageEntry(matchingImage)
            //TODO ADD MACRO IMAGE TO PROJECT and open SECOND image

            qupathGUI.refreshProject()
            //Check if the tiles should be deleted from the collection folder
            if (preferences.tileHandling == "Delete")
                utilityFunctions.deleteTilesAndFolder(tempTileDirectory)
            if (preferences.tileHandling == "Zip") {
                utilityFunctions.zipTilesAndMove(tempTileDirectory)
                utilityFunctions.deleteTilesAndFolder(tempTileDirectory)
            }


        }
    }

    // Helper method to check if input is numeric
    private static boolean isValidInput(String input) {
        return input.matches("\\d*");
    }


    private static GridPane createContent3() {
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

        //addToGrid(pane, new Label('Slide flipped:'), slideFlippedCheckBox, row++)
        addToGrid(pane, new Label('Tissue detection script:'), groovyScriptField, row++)
        // Add new components for pixel size and non-isotropic pixels checkbox on the same line
        HBox pixelSizeBox = new HBox(10);
        pixelSizeBox.getChildren().addAll(new Label('Pixel Size XY um:'), pixelSizeField, nonIsotropicCheckBox);
        addToGrid(pane, pixelSizeBox, row++);
        // Add new components for "Upper left XY coordinate"
        //Label upperLeftLabel = new Label("Upper left XY coordinate")
        //pane.add(upperLeftLabel, 0, row); // Span multiple columns if needed

        //addToGrid(pane, new Label('X coordinate:'), x1Field, ++row);
        //addToGrid(pane, new Label('Y coordinate:'), y1Field, ++row);


        return pane
    }

    static boolean createGUI4() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.initModality(Modality.NONE);
        dlg.setTitle("Identify Location");
        dlg.setHeaderText("Please identify a location of interest in the Live view in uManager and draw an unclassified rectangle in QuPath that matches that FOV.\n This will be used for matching QuPath's coordinate system to the microscope stage coordinate system, so be as careful as you can!");
        // Add buttons to the dialog
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result;
        boolean validRectangle = false;

        while (!validRectangle) {
            // Show the dialog and wait for the user response
            result = dlg.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Check for expected rectangle
                List expectedRectangles = getAnnotationObjects().stream()
                        .filter(a -> a.getPathClass() == null && a.getROI() instanceof qupath.lib.roi.RectangleROI)
                        .collect(Collectors.toList());

                if (expectedRectangles.size() != 1) {
                    // Use utilityFunctions to show a warning
                    utilityFunctions.showAlertDialog("There needs to be exactly one unclassified rectangle.");
                } else {
                    validRectangle = true;
                }
            } else {
                // User cancelled or closed the dialog
                return false;
            }
        }
        return true
    }


    static boolean createGUI5() {
        List<String> choices = Arrays.asList("Yes", "Use adjusted position");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Yes", choices);
        dialog.initModality(Modality.NONE);
        dialog.setTitle("Position Confirmation");
        dialog.setHeaderText("Is the current position accurate? Compare with the uManager live view!\n The first time this dialog shows up, it should select the center of the top row! \n The second time, it should select the center of the left-most column!");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            return "Use adjusted position".equals(result.get());
        }

        // If no choice is made (e.g., dialog is closed), you can decide to return false or handle it differently
        return false;
    }

}
