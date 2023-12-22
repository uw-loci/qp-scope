package qupath.ext.qp_scope.functions

import javafx.scene.Node
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.stage.Modality
import org.slf4j.LoggerFactory
import qupath.ext.qp_scope.utilities.utilityFunctions
import qupath.lib.gui.dialogs.Dialogs
import groovy.io.FileType
import qupath.lib.images.writers.ome.OMEPyramidWriter
import qupath.lib.projects.Project
import qupath.lib.gui.QuPathGUI
import qupath.lib.scripting.QP
import qupath.lib.gui.scripting.QPEx
import qupath.ext.basicstitching.stitching.stitchingImplementations

import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

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
    static TextField pythonScriptField = new TextField(preferences.installation)
    static TextField projectsFolderField = new TextField(preferences.projects)
    static TextField sampleLabelField = new TextField("First_Test")  // New field for sample label

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
            if (dataCheck){
                Project currentQuPathProject= utilityFunctions.createProjectFolder(projectsFolderPath, sampleLabel, preferences.firstScanType)
                def scanTypeWithIndex = utilityFunctions.getUniqueFolderName(projectsFolderPath + File.separator + sampleLabel + File.separator + preferences.firstScanType)
                def tempTileDirectory = projectsFolderPath + File.separator + sampleLabel+File.separator+scanTypeWithIndex
                def logger = LoggerFactory.getLogger(QuPathGUI.class)
                logger.info(tempTileDirectory)
                //Reduce the number of sent args
                def boundingBox = "{$x1}, {$y1}, {$x2}, {$y2}"
                // scanTypeWithIndex will be the name of the folder where the tiles will be saved to

                List args = [pythonScriptPath, projectsFolderPath, sampleLabel,scanTypeWithIndex,annotationJsonFileLocation, boundingBox ]
                utilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, args)


                String stitchedImageOutputFolder = projectsFolderPath + File.separator + sampleLabel + File.separator + "SlideImages"
                //TODO Need to check if stitching is successful, provide error
                //stitchingImplementations.stitchCore(stitchingType, folderPath, compressionType, pixelSize, downsample, matchingString)
                //TODO add output folder to stitchCore
                String stitchedImagePathStr = stitchingImplementations.stitchCore("Coordinates in TileCoordinates.txt file", projectsFolderPath + File.separator + sampleLabel, stitchedImageOutputFolder, "J2K_LOSSY", 0, 1, scanTypeWithIndex)


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
        addToGrid(pane, new Label('.py file path:'), pythonScriptField, row++)
        addToGrid(pane, new Label('Projects path:'), projectsFolderField, row++)
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
            def tempTileDirectory = projectsFolderPath + File.separator + sampleLabel+File.separator+scanTypeWithIndex
            logger.info("Scan type with index: " + scanTypeWithIndex)
            logger.info(tempTileDirectory)
            logger.info("Creating json")
            annotationJsonFileLocation = utilityFunctions.createAnnotationJson(projectsFolderPath, sampleLabel, scanTypeWithIndex)

            List args = [pythonScriptPath, projectsFolderPath, sampleLabel,scanTypeWithIndex, annotationJsonFileLocation]
            //TODO how can we distinguish between a hung python run and one that is taking a long time? - possibly check for new files in target folder?
            utilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, args)
            //utilityFunctions.runPythonCommand(virtualEnvPath,  "C:\\ImageAnalysis\\python\\py_dummydoc.py", args)
            logger.info("Finished Python Command")
            String stitchedImageOutputFolder = projectsFolderPath + File.separator + sampleLabel + File.separator + "SlideImages"
            //TODO Need to check if stitching is successful, provide error
            //TODO get pixel size from somewhere???

            //stitchingImplementations.stitchCore(stitchingType, folderPath, compressionType, pixelSize, downsample, matchingString)
            logger.info("Begin stitching")
            String stitchedImagePathStr =stitchingImplementations.stitchCore("Coordinates in TileCoordinates.txt file", projectsFolderPath + File.separator + sampleLabel, stitchedImageOutputFolder, "J2K_LOSSY", 0, 1, scanTypeWithIndex)
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
        addToGrid(pane, new Label('.py file path:'), pythonScriptField, row++)
        addToGrid(pane, new Label('Projects path:'), projectsFolderField, row++)
        // Listener for the checkbox

        return pane
    }
}
