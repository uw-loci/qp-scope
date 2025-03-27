package qupath.ext.template

import javafx.beans.property.BooleanProperty
import javafx.beans.property.Property
import javafx.scene.Scene
import javafx.scene.control.MenuItem
import javafx.stage.Stage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import qupath.ext.template.ui.InterfaceController
import qupath.fx.dialogs.Dialogs
import qupath.fx.prefs.controlsfx.PropertyItemBuilder
import qupath.lib.common.Version
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.extensions.GitHubProject
import qupath.lib.gui.extensions.QuPathExtension
import qupath.lib.gui.prefs.PathPrefs
import java.util.ResourceBundle

class DemoExtension implements QuPathExtension, GitHubProject {

    // Groovy classes are public by default
    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.template.ui.strings")
    private static final Logger logger = LoggerFactory.getLogger(DemoExtension)
    private static final String EXTENSION_NAME = resources.getString("name")
    private static final String EXTENSION_DESCRIPTION = resources.getString("description")
    private static final Version EXTENSION_QUPATH_VERSION = Version.parse("v0.5.0")
    private static final GitHubRepo EXTENSION_REPOSITORY = GitHubRepo.create(EXTENSION_NAME, "myGitHubUserName", "myGitHubRepo")

    private boolean isInstalled = false

    // Persistent preferences created with PathPrefs.
    private static final BooleanProperty enableExtensionProperty = PathPrefs.createPersistentPreference("enableExtension", true)
    private static final Property<Integer> integerOption = PathPrefs.createPersistentPreference("demo.num.option", 1).asObject()

    // Exposes the persistent integer option
    static Property<Integer> integerOptionProperty() {
        integerOption
    }

    private Stage stage

    @Override
    void installExtension(QuPathGUI qupath) {
        if (isInstalled) {
            logger.debug("{} is already installed", getName())
            return
        }
        isInstalled = true
        addPreferenceToPane(qupath)
        addMenuItem(qupath)
    }

    /**
     * Adds a persistent preference to the QuPath preferences pane.
     * Groovy lets you use property notation for getters/setters, making the code more concise.
     */
    private void addPreferenceToPane(QuPathGUI qupath) {
        def propertyItem = new PropertyItemBuilder<>(enableExtensionProperty, Boolean)
                .name(resources.getString("menu.enable"))
                .category("Demo extension")
                .description("Enable the demo extension")
                .build()
        // Accessing nested properties using Groovy's property notation.
        qupath.preferencePane.propertySheet.items.add(propertyItem)
    }

    /**
     * Adds a new menu item to the QuPath menu.
     * Note the use of a closure instead of a Java lambda.
     */
    private void addMenuItem(QuPathGUI qupath) {
        def menu = qupath.getMenu("Extensions>${EXTENSION_NAME}", true)
        def menuItem = new MenuItem("My menu item")
        menuItem.setOnAction({ createStage() } as javafx.event.EventHandler)
        menuItem.disableProperty().bind(enableExtensionProperty.not())
        menu.items.add(menuItem)
    }

    /**
     * Creates a new stage with the JavaFX FXML interface.
     * Groovy simplifies exception handling by not requiring declared exceptions.
     */
    private void createStage() {
        if (!stage) {
            try {
                stage = new Stage()
                def scene = new Scene(InterfaceController.createInstance())
                // Using Groovy's property syntax instead of setter methods.
                stage.initOwner(QuPathGUI.instance.stage)
                stage.title = resources.getString("stage.title")
                stage.scene = scene
                stage.resizable = false
            } catch (IOException e) {
                Dialogs.showErrorMessage(resources.getString("error"), resources.getString("error.gui-loading-failed"))
                logger.error("Unable to load extension interface FXML", e)
            }
        }
        stage.show()
    }

    @Override
    String getName() {
        EXTENSION_NAME
    }

    @Override
    String getDescription() {
        EXTENSION_DESCRIPTION
    }

    @Override
    Version getQuPathVersion() {
        EXTENSION_QUPATH_VERSION
    }

    @Override
    GitHubRepo getRepository() {
        EXTENSION_REPOSITORY
    }
}
