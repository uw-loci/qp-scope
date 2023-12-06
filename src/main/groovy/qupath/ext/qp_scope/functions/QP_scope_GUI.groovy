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

class QP_scope_GUI {

    // Existing text fields
    static TextField x1Field = new TextField("");
    static TextField y1Field = new TextField("");
    static TextField x2Field = new TextField("");
    static TextField y2Field = new TextField("");
    static TextField scanBox = new TextField("");

    // New text fields for Python environment, script path, and sample label
    static TextField virtualEnvField = new TextField("");
    static TextField pythonScriptField = new TextField("");
    static TextField sampleLabelField = new TextField("");  // New field for sample label

    static void createGUI() {
        // Create the dialog
        def dlg = new Dialog<ButtonType>();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("qp_scope");
        dlg.setHeaderText("Enter details:");

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
            def virtualEnvPath = virtualEnvField.getText();
            def pythonScriptPath = pythonScriptField.getText();

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
                utilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, sampleLabel, x1, y1, x2, y2);
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

        return pane;
    }

    private static void addToGrid(GridPane pane, Node label, Node control, int rowIndex) {
        pane.add(label, 0, rowIndex);
        pane.add(control, 1, rowIndex);
    }
}
