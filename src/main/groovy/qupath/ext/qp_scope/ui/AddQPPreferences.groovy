package qupath.ext.qp_scope.ui
//TODO Set logging level?

import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.StringProperty
import javafx.beans.property.DoubleProperty
import javafx.scene.control.TextField
import qupath.lib.gui.scripting.QPEx
import qupath.fx.prefs.controlsfx.PropertyItemBuilder;
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.prefs.PathPrefs
import qupath.lib.images.writers.ome.OMEPyramidWriter
import qupath.lib.scripting.QP
/**
 * A class to add and manage various preferences for a QuPath extension.
 * This class demonstrates how to use persistent preferences in QuPath,
 * allowing for different types of properties such as String, Double, and List.
 */
class AddQPPreferences {
    static AddQPPreferences instance
    static AddQPPreferences getInstance() {
        if (!instance) {
            instance = new AddQPPreferences()
        }
        return instance
    }

    // A list to hold the details of each preference. Each preference is represented as a map.
    List<Map<String, Object>> preferencesList = []
    //TODO probably bad to have this twice, but not sure it's worth passing?
    private static final String EXTENSION_NAME = "Microscopy in QuPath"

    AddQPPreferences() {
        // Initialize and add the properties to the preferencesList
        initializePreferences()

        // This is just an example. In actual usage, you would likely call addPreferencesToPane(qupath)
        // from the method where you have a valid instance of QuPathGUI ready, such as in an installExtension method.
    }

    /**
     * Initializes properties and adds them to the list with all necessary details.
     * Each map entry in the list contains keys for 'key', 'property', 'type', 'name', and 'description',
     * which are used to add the preference to the QuPath GUI.
     */
    private static void initializePreferences() {


// Flip macro image horizontally
        BooleanProperty isFlippedXProperty = PathPrefs.createPersistentPreference("isFlippedXProperty", false);
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(isFlippedXProperty, Boolean.class)
                        .name("Flip macro image X")
                        .category(EXTENSION_NAME)
                        .description("Allows the slide to be flipped horizontally so that the coordinates can be matched correctly with the stage.")
                        .build()
        );
        // Flip macro image vertically
        BooleanProperty isFlippedYProperty = PathPrefs.createPersistentPreference("isFlippedYProperty", false);
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(isFlippedYProperty, Boolean.class)
                        .name("Flip macro image Y")
                        .category(EXTENSION_NAME)
                        .description("Allows the slide to be flipped vertically so that the coordinates can be matched correctly with the stage.")
                        .build()
        );

        // Stage axis is inverted relative to QuPath
        BooleanProperty isInvertedXProperty = PathPrefs.createPersistentPreference("isInvertedXProperty", false);
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(isInvertedXProperty, Boolean.class)
                        .name("Inverted X stage")
                        .category(EXTENSION_NAME)
                        .description("Stage axis is inverted in X relative to QuPath.")
                        .build()
        );
        // Stage axis is inverted relative to QuPath
        BooleanProperty isInvertedYProperty = PathPrefs.createPersistentPreference("isInvertedYProperty", true);
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(isInvertedYProperty, Boolean.class)
                        .name("Inverted Y stage")
                        .category(EXTENSION_NAME)
                        .description("Stage axis is inverted in Y relative to QuPath.")
                        .build()
        );
// PycroManager Script Path
        StringProperty pycromanagerProperty = PathPrefs.createPersistentPreference("pycromanagerProperty", "C:\\Users\\lociuser\\Codes\\smart-wsi-scanner\\minimal_qupathrunner.py");
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(pycromanagerProperty, String.class)
                        .propertyType(PropertyItemBuilder.PropertyType.DIRECTORY)
                        .name("PycroManager Path")
                        .category(EXTENSION_NAME)
                        .description("Path to the PycroManager script used for controlling microscopes.")
                        .build()
        );

// Python Environment Path
        StringProperty pythonEnvironmentProperty = PathPrefs.createPersistentPreference("pythonEnvironmentProperty", "C:\\Users\\lociuser\\miniconda3\\envs\\spath");
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(pythonEnvironmentProperty, String.class)
                        .propertyType(PropertyItemBuilder.PropertyType.DIRECTORY)
                        .name("Python Environment")
                        .category(EXTENSION_NAME)
                        .description("Path to the Python environment.")
                        .build()
        );

// Projects Folder Path
        StringProperty projectsFolderProperty = PathPrefs.createPersistentPreference("projectsFolderProperty", "C:\\Users\\lociuser\\Codes\\MikeN\\data\\slides");
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(projectsFolderProperty, String.class)
                        .propertyType(PropertyItemBuilder.PropertyType.DIRECTORY)
                        .name("Projects Folder")
                        .category(EXTENSION_NAME)
                        .description("Path to the projects folder.")
                        .build()
        );

// Extension Path
        StringProperty extensionPathProperty = PathPrefs.createPersistentPreference("extensionPathProperty", "C:\\Users\\lociuser\\Codes\\MikeN\\qp_scope");
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(extensionPathProperty, String.class)
                        .propertyType(PropertyItemBuilder.PropertyType.DIRECTORY)
                        .name("Extension Location")
                        .category(EXTENSION_NAME)
                        .description("Path to the extension directory in order to find included scripts.")
                        .build()
        );


// Tissue Detection Script

        def groovyScript = extensionPathProperty.getValue().toString()+"/src/main/groovyScripts/DetectTissue.groovy"

        StringProperty tissueDetectionScriptProperty = PathPrefs.createPersistentPreference("tissueDetectionScriptProperty", groovyScript);
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(tissueDetectionScriptProperty, String.class)
                .propertyType(PropertyItemBuilder.PropertyType.FILE)
                        .name("Tissue Detection Script")
                        .category(EXTENSION_NAME)
                        .description("Tissue detection script.")
                        .build()
        );

// First Scan Type
        StringProperty firstImagingModeProperty = PathPrefs.createPersistentPreference("firstImagingModeProperty", "4x_bf");
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(firstImagingModeProperty, String.class)
                        .name("First Scan Type")
                        .category(EXTENSION_NAME)
                        .description("Type of the first scan (e.g., magnification and method).")
                        .build()
        );

// Second Scan Type
        StringProperty secondImagingModeProperty = PathPrefs.createPersistentPreference("secondImagingModeProperty", "20x_bf");
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(secondImagingModeProperty, String.class)
                        .name("Second Scan Type")
                        .category(EXTENSION_NAME)
                        .description("Type of the second scan (e.g., magnification and method).")
                        .build()
        );

// Tile Handling Method
        StringProperty tileHandlingProperty = PathPrefs.createPersistentPreference("tileHandlingProperty", "None");
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(tileHandlingProperty, String.class)
                        .propertyType(PropertyItemBuilder.PropertyType.CHOICE)
                        .name("Tile Handling Method")
                        .category(EXTENSION_NAME)
                        .choices(["None", "Zip", "Delete"])
                        .description("Specifies how tiles are handled during scanning. " +
                                "\n'None' will leave the files in the folder where they were written." +
                                "\n'Zip' will compress the tiles and their associated TileConfiguration file into a file and place it in a separate folder." +
                                "\n'Delete' will delete the tiles and keep NO COPIES. Only use this if you are confident in your system and need the space.")
                        .build()
        );


// Pixel Size for First Scan Type
        DoubleProperty pixelSizeFirstImagingModeProperty = PathPrefs.createPersistentPreference("pixelSizeFirstImagingModeProperty", 1.105);
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(pixelSizeFirstImagingModeProperty, Double.class)
                        .name("1st scan pixel size um")
                        .category(EXTENSION_NAME)
                        .description("Pixel size for the first scan type, in micrometers.")
                        .build()
        );



// Pixel Size for Second Scan Type
        DoubleProperty pixelSizeSecondImagingModeProperty = PathPrefs.createPersistentPreference("pixelSizeSecondImagingModeProperty", 0.5);
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(pixelSizeSecondImagingModeProperty, Double.class)
                        .name("2nd scan pixel size um")
                        .category(EXTENSION_NAME)
                        .description("Pixel size for the second scan type, in micrometers.")
                        .build()
        );

// Camera Frame Width in Pixels
        IntegerProperty cameraFrameWidthPxProperty = PathPrefs.createPersistentPreference("cameraFrameWidthPxProperty", 1392);
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(cameraFrameWidthPxProperty, Integer.class)
                        .name("Camera Frame Width #px")
                        .category(EXTENSION_NAME)
                        .description("Width of the camera frame in pixels.")
                        .build()
        );

// Camera Frame Height in Pixels
        IntegerProperty cameraFrameHeightPxProperty = PathPrefs.createPersistentPreference("cameraFrameHeightPxProperty", 1040);
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(cameraFrameHeightPxProperty, Integer.class)
                        .name("Camera Frame Height #px")
                        .category(EXTENSION_NAME)
                        .description("Height of the camera frame in pixels.")
                        .build()
        );

// Tile Overlap Percent
        DoubleProperty tileOverlapPercentProperty = PathPrefs.createPersistentPreference("tileOverlapPercentProperty", 0.0);
        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(tileOverlapPercentProperty, Double.class)
                        .name("Tile Overlap Percent")
                        .category(EXTENSION_NAME)
                        .description("Percentage of overlap between adjacent tiles.")
                        .build()
        );


// Create the persistent property for OMEPyramidWriter.CompressionType
        ObjectProperty<OMEPyramidWriter.CompressionType> compressionType = PathPrefs.createPersistentPreference(
                "compressionType",
                OMEPyramidWriter.CompressionType.DEFAULT, // Assuming DEFAULT is an acceptable default value
                OMEPyramidWriter.CompressionType.class);

// Then add to the preference pane

        QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems().add(
                new PropertyItemBuilder<>(compressionType, OMEPyramidWriter.CompressionType.class)
                        .propertyType(PropertyItemBuilder.PropertyType.CHOICE)
                        .name("Compression type")
                        .category(EXTENSION_NAME) // Adjust the category as needed
                        .choices(Arrays.asList(OMEPyramidWriter.CompressionType.values()))
                        .description("Type of compression used for final images.")
                        .build()
        );

    }

    public Property getProperty(String name) {
        // Assuming preferencesList is a list of maps or a similar structure
        // that holds your properties.
        def pref = preferencesList.find { it.name == name }
        return pref?.property as Property
    }

}



class AutoFillPersistentPreferences{

    //private AutoFillPersistentPreferences options = AutoFillPersistentPreferences.getInstance();
    private static StringProperty slideLabelSaved = PathPrefs.createPersistentPreference("SlideLabel", "First_Test");

    public static String getSlideLabel(){
        return slideLabelSaved.value
    }
    public static void setSlideLabel(final String slideLabelSaved){
        this.slideLabelSaved.value = slideLabelSaved
    }

    private static StringProperty macroImagePixelSizeInMicrons = PathPrefs.createPersistentPreference("macroImagePixelSizeInMicrons", "7.2");

    public static String getMacroImagePixelSizeInMicrons(){
        return macroImagePixelSizeInMicrons.value
    }
    public static void setMacroImagePixelSizeInMicrons(final String macroImagePixelSizeInMicrons){
        this.macroImagePixelSizeInMicrons.value = macroImagePixelSizeInMicrons
    }

    private static StringProperty classListSaved = PathPrefs.createPersistentPreference("classList", "Tumor, Stroma, Immune");

    public static String getClassList(){
        return classListSaved.value
    }
    public static void setClassList(final String classListSaved){
        this.classListSaved.value = classListSaved
    }

    private static StringProperty modalityForAutomationSaved = PathPrefs.createPersistentPreference("modalityForAutomation", "20x_bf");

    public static String getModalityForAutomation(){
        return modalityForAutomationSaved.value
    }
    public static void setModalityForAutomation(final String modalityForAutomationSaved){
        this.modalityForAutomationSaved.value = modalityForAutomationSaved
    }
//TODO shift this to an actual preference so it can be a file dialog
    private static StringProperty analysisScriptForAutomationSaved = PathPrefs.createPersistentPreference("analysisScriptForAutomation", "DetectROI.groovy");

    public static String getAnalysisScriptForAutomation(){
        return analysisScriptForAutomationSaved.value
    }
    public static void setAnalysisScriptForAutomation(final String analysisScriptForAutomationSaved){
        this.analysisScriptForAutomationSaved.value = analysisScriptForAutomationSaved
    }

}