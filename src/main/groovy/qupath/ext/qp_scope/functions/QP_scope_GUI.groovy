package qupath.ext.qp_scope.functions

import javafx.scene.Node
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.stage.Modality
import qupath.ext.qp_scope.utilities.utilityFunctions
import qupath.lib.gui.dialogs.Dialogs
import groovy.io.FileType
import qupath.lib.projects.Project
import qupath.lib.gui.QuPathGUI
import qupath.lib.scripting.QP
import qupath.lib.gui.scripting.QPEx
import qupath.ext.basicstitching.BasicStitchingExtension


public class QP_scope_GUI {

    // Existing text fields
    static TextField x1Field = new TextField("");
    static TextField y1Field = new TextField("");
    static TextField x2Field = new TextField("");
    static TextField y2Field = new TextField("");
    static TextField scanBox = new TextField("20,25,30,35");
    static preferences = utilityFunctions.getPreferences()

    // New text fields for Python environment, script path, and sample label
    static TextField virtualEnvField = new TextField(preferences.environment);
    static TextField pythonScriptField = new TextField(preferences.installation);
    static TextField projectsFolderField = new TextField(preferences.projects);
    static TextField sampleLabelField = new TextField("First_Test");  // New field for sample label

    static void createGUI() {
        // Create the dialog
        def dlg = new Dialog<ButtonType>();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("qp_scope");
        dlg.setHeaderText("Enter details (LOOK MA! " + BasicStitchingExtension.class.getName() + "!):");

        // Set the content
        dlg.getDialogPane().setContent(createContent());

        // Add Okay and Cancel buttons
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Show the dialog and capture the response
        def result = dlg.showAndWait();

        // Handling the response
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Retrieve values from text fields
            def sampleLabel = sampleLabelField.getText();
            def x1 = x1Field.getText();
            def y1 = y1Field.getText();
            def x2 = x2Field.getText();
            def y2 = y2Field.getText();
            def virtualEnvPath = virtualEnvField.getText()
            def pythonScriptPath = pythonScriptField.getText()
            def projectsFolderPath = projectsFolderField.getText()


            // Handle full bounding box input
            def boxString = scanBox.getText();
            if (boxString != "") {
                def values = boxString.replaceAll("[^0-9.,]", "").split(",");
                if (values.length == 4) {
                    x1 = values[0];
                    y1 = values[1];
                    x2 = values[2];
                    y2 = values[3];
                }
            }

            // Check if any value is empty
            if ([sampleLabel, x1, y1, x2, y2, virtualEnvPath, pythonScriptPath].any { it == null || it.isEmpty() }) {
                Dialogs.showWarningNotification("Warning!", "Incomplete data entered.");
            } else {
                Project currentQuPathProject= utilityFunctions.createProjectFolder(projectsFolderPath, sampleLabel, preferences.firstScanType)

                utilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, projectsFolderPath, sampleLabel, x1, y1, x2, y2);

                //TODO figure out how to call stitching function in other plugin

                utilityFunctions.showAlertDialog("Wait and complete stitching in other version of QuPath")
                String stitchedImagePathStr = projectsFolderPath + File.separator + sampleLabel + File.separator + "SlideImages" + File.separator + preferences.firstScanType + sampleLabel + ".ome.tif";
                File stitchedImagePath = new File(stitchedImagePathStr);
                utilityFunctions.addImageToProject(stitchedImagePath, currentQuPathProject)

                //open the newly created project
                //https://qupath.github.io/javadoc/docs/qupath/lib/gui/QuPathGUI.html#setProject(qupath.lib.projects.Project)
                def qupathGUI = QPEx.getQuPath()

                qupathGUI.setProject(currentQuPathProject)
                //Find the existing images - there should only be one since the project was just created
                def firstImage = currentQuPathProject.getImageList()[0]

                //Open the first image
                //https://qupath.github.io/javadoc/docs/qupath/lib/gui/QuPathGUI.html#openImageEntry(qupath.lib.projects.ProjectImageEntry)
                qupathGUI.openImageEntry(firstImage)

                //}
            }
        }
    }

    private static GridPane createContent() {
        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);
        def row = 0;

        // Add new component for Sample Label
        addToGrid(pane, new Label('Sample Label:'), sampleLabelField, row++);

        // Add existing components to the grid
        addToGrid(pane, new Label('X1:'), x1Field, row++);
        addToGrid(pane, new Label('Y1:'), y1Field, row++);
        addToGrid(pane, new Label('X2:'), x2Field, row++);
        addToGrid(pane, new Label('Y2:'), y2Field, row++);
        addToGrid(pane, new Label('Full bounding box:'), scanBox, row++);

        // Add components for Python environment and script path
        addToGrid(pane, new Label('Python Virtual Env Location:'), virtualEnvField, row++);
        addToGrid(pane, new Label('.py file path:'), pythonScriptField, row++);
        addToGrid(pane, new Label('Projects path:'), projectsFolderField, row++);

        return pane;
    }

    private static void addToGrid(GridPane pane, Node label, Node control, int rowIndex) {
        pane.add(label, 0, rowIndex);
        pane.add(control, 1, rowIndex);
    }

}
