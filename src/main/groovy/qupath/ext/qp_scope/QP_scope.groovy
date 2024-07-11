package qupath.ext.qp_scope


import javafx.scene.control.MenuItem
import org.slf4j.LoggerFactory
import qupath.ext.qp_scope.ui.QP_scope_GUI
import qupath.lib.common.Version
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.extensions.GitHubProject
import qupath.lib.gui.extensions.QuPathExtension
import qupath.ext.qp_scope.ui.AddQPPreferences
import qupath.ext.qp_scope.tests.CoordinateTransformationTest;
import javafx.application.Platform;
import qupath.ext.qp_scope.utilities.MinorFunctions
import java.nio.file.Files
import qupath.lib.gui.scripting.QPEx
import java.nio.file.Path
import java.nio.file.Paths
import qupath.ext.qp_scope.utilities.MicroscopeConfigManager
/**
 * Built from the QuPath extension template - an extension to control a microscope through a Python interface
 */
class QP_scope implements QuPathExtension, GitHubProject {
    def logger = LoggerFactory.getLogger(QP_scope.class)
    // Setting the variables here is enough for them to be available in the extension
    private static final String EXTENSION_NAME = "Microscopy in QuPath"
    private static final String EXTENSION_DESCRIPTION = "Interact with a microscope from QuPath via Python interfaces like PycroManager or PyMMCore"
    private static final Version EXTENSION_QUPATH_VERSION = Version.parse("v0.5.0");
    /**
     * GitHub repo that your extension can be found at.
     */
    private static final GitHubRepo EXTENSION_REPOSITORY = GitHubRepo.create(
            EXTENSION_NAME, "MichaelSNelson", "qp-scope");

    @Override
    void installExtension(QuPathGUI qupath) {

        // Initialize and load config
        try {
            addMenuItems(qupath)
            AddQPPreferences.getInstance();
            def preferences = QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems()
            String pycromanagerFilePath = preferences.find { it.getName() == "PycroManager Path" }.getValue().toString()
            logger.info("Retrieved PycroManager Path: $pycromanagerFilePath")

            if (pycromanagerFilePath != null) {
                Path filePath = Paths.get(pycromanagerFilePath);
                Path folderPath = filePath.getParent();
                //TODO need a preference for the microscope name
                //Nice to be a dropdown of config_NAME options in the config folder

                Path yamlPath = folderPath.resolve("config/config_CAMM.yml");
                logger.info("Constructed YAML Path: $yamlPath")

                if (Files.exists(yamlPath)) {
                    def configManager = MicroscopeConfigManager.getInstance(yamlPath.toString())
                    logger.info("Config Manager initialized.")
                    // Retrieve specific configuration items
                    def lowerXlimitStage = configManager.getConfigItem("stage", "xlimit", "low")
                    def objectiveLens = configManager.getConfigItem("objectiveLens")
                    def lampIntensity = configManager.getConfigItem("imagingMode", "lampIntensity")

                    logger.info("lowerXlimitStage: $lowerXlimitStage")
                    logger.info("objectiveLens: $objectiveLens")
                    logger.info("lampIntensity: $lampIntensity")
                } else {
                    logger.warn("YAML file not found at: $yamlPath")
                    Platform.runLater(() -> {
                        MinorFunctions.showAlertDialog("YAML file not found at: $yamlPath")
                    });
                }
            } else {
                logger.error("PycroManager Path is null or not set.")
                Platform.runLater(() -> {
                    MinorFunctions.showAlertDialog("PycroManager Path is not configured. Please check your settings.")
                });
            }
        } catch (Exception e) {
            logger.error("Error during extension installation", e)
        }

        logger.info("QP_scope extension installation completed.")

    }


    private void addMenuItems(QuPathGUI qupath) {

        // Check for dependencies and QuPath version
        logger.info("QuPath Version")
        logger.info(getQuPathVersion().toString())
        // TODO: how to check if version is supported?

        // Get or create the menu
        def menu = qupath.getMenu("Extensions>${name}", true)

        // Bounding box as input
        def qpScope1 = new MenuItem("First scan type - Use bounding box")
        // TODO: tooltip
        qpScope1.setOnAction(e -> {
            // TODO: check preferences for all necessary entries, and check for micromanager running+version
            // search java app with a subprocesses for MicroManager +version number
            QP_scope_GUI.boundingBoxInputGUI()
        })


        // Macro or overview image as input
        def qpScope2 = new MenuItem("First scan type - Use current image")
        // TODO: tooltip
        qpScope2.setOnAction(e -> {
            // TODO: check preferences for all necessary entries
            QP_scope_GUI.macroImageInputGUI()
        })

        // Disable the menu option that requires an active image, if there is no active image
        qpScope2.disableProperty().bind(qupath.imageDataProperty().isNull());


        // Third menu item - "Second scan modality"
        def qpScope3 = new MenuItem("Second scan type - Scan non \'Tissue\' annotations ")
        // TODO: tooltip
        qpScope3.setOnAction(e -> {
            QP_scope_GUI.secondImagingModeGUI()

        })
        // Fourth menu item - "Use current image as macro view"
        def qpScope4 = new MenuItem("Dummy menu option for troubleshooting")
        // TODO: tooltip
        qpScope4.setOnAction(e -> {

            new Thread(() -> {
                logger.info("Starting Coordinate Transformation Test...")
                CoordinateTransformationTest.main(null)  // Execute the test in a separate thread
                Platform.runLater(() -> logger.info("Test completed."))
            }).start();
            //QP_scope_GUI.testGUI()
            // Directly toggle the property to test listener reaction
//            def qppreferences = QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems()
//            logger.info("Toggling enableExtension");
//            def enableExtension = qppreferences.find{it.getName() == "Enable my extension"}
//            enableExtension.setValue(!enableExtension.getValue());
        })
        // Add the menu items to the menu
        menu.getItems() << qpScope1
        menu.getItems() << qpScope2
        menu.getItems() << qpScope3
        menu.getItems() << qpScope4
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    @Override
    public String getDescription() {
        return EXTENSION_DESCRIPTION;
    }

    @Override
    public Version getQuPathVersion() {
        return EXTENSION_QUPATH_VERSION;
    }

    @Override
    public GitHubRepo getRepository() {
        return EXTENSION_REPOSITORY;
    }

}
