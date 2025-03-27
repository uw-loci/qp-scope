package qupath.ext.template.ui

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.Spinner
import javafx.scene.layout.VBox
import qupath.ext.template.DemoExtension
import qupath.fx.dialogs.Dialogs
import java.util.ResourceBundle

// In Groovy classes are public by default—no need for an explicit public modifier.
class InterfaceController extends VBox {

    // Static field (semicolons not needed)
    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.template.ui.strings")

    @FXML
    Spinner<Integer> integerOptionSpinner  // Field declaration remains similar

    /**
     * Factory method to create an instance.
     * No checked exceptions need to be declared in Groovy.
     */
    static InterfaceController createInstance() {
        new InterfaceController()
    }

    /**
     * Private constructor that loads the FXML.
     * Note: The 'throws IOException' clause is omitted because Groovy does not require it.
     */
    private InterfaceController() {
        def url = InterfaceController.class.getResource("interface.fxml")
        def loader = new FXMLLoader(url, resources)
        loader.setRoot(this)
        loader.setController(this)
        loader.load()

        // Using Groovy closures instead of Java lambda expressions.
        integerOptionSpinner.valueFactory.valueProperty().bindBidirectional(DemoExtension.integerOptionProperty())
        integerOptionSpinner.valueFactory.valueProperty().addListener { observableValue, oldValue, newValue ->
            Dialogs.showInfoNotification(
                    resources.getString("title"),
                    String.format(resources.getString("option.integer.option-set-to"), newValue)
            )
        }
    }

    @FXML
    void runDemoExtension() {
        Dialogs.showInfoNotification(
                resources.getString("run.title"),
                resources.getString("run.message")
        )
    }
}
