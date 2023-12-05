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


    // New text fields for the coordinates
    static TextField x1Field = new TextField("")
    static TextField y1Field = new TextField("")
    static TextField x2Field = new TextField("")
    static TextField y2Field = new TextField("")
    static TextField scanBox = new TextField("")

    static void createGUI() {
        // Create the dialog
        def dlg = new Dialog<ButtonType>()
        dlg.initModality(Modality.APPLICATION_MODAL)
        dlg.setTitle("qp_scope")
        dlg.setHeaderText("Enter coordinates:")

        // Set the content
        dlg.getDialogPane().setContent(createContent())

        // Add Okay and Cancel buttons
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

        // Show the dialog and capture the response
        def result = dlg.showAndWait()

        // Handling the response
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Retrieve values from text fields
            def x1 = x1Field.getText()
            def y1 = y1Field.getText()
            def x2 = x2Field.getText()
            def y2 = y2Field.getText()
            def boxString = scanBox.getText()
            if (boxString != "") {
                // Remove non-numeric/non-comma characters and split by comma
                def values = boxString.replaceAll("[^0-9.,]", "").split(",")
                if (values.length == 4) {
                    x1 = values[0]
                    y1 = values[1]
                    x2 = values[2]
                    y2 = values[3]
                }
            }

            // Check if any value is empty and show warning
            if ([x1, y1, x2, y2].any { it == null || it.isEmpty() }) {
                // Show warning if the format is incorrect
                Dialogs.showWarningNotification("Warning!", "Incomplete data to define bounding box.")
            } else {
                // Call the specified function
                String virtualEnvPath2 = "C:/Anaconda/envs/paquo"
                String pythonScriptPath = "C:/ImageAnalysis/python/py_dummydoc.py"
                utilityFunctions.runPythonCommand(virtualEnvPath2,pythonScriptPath, x1, y1, x2, y2)
            }
        }
    }

    private static GridPane createContent() {
        GridPane pane = new GridPane()
        pane.setHgap(10)
        pane.setVgap(10)
        def row = 0
        // Add coordinate components to the grid
        addToGrid(pane, new Label('X1:') as Node, x1Field as Node, row++)
        addToGrid(pane, new Label('Y1:') as Node, y1Field as Node, row++)
        addToGrid(pane, new Label('X2:') as Node, x2Field as Node, row++)
        addToGrid(pane, new Label('Y2:') as Node, y2Field as Node, row++)
        addToGrid(pane, new Label('Full bounding box:') as Node, scanBox as Node, row++)

        return pane
    }

    private static void addToGrid(GridPane pane, Node label, Node control, int rowIndex) {
        pane.add(label as Node, 0, rowIndex)
        pane.add(control as Node, 1, rowIndex)
    }
}