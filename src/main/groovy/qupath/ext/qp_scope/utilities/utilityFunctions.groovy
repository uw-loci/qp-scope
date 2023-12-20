package qupath.ext.qp_scope.utilities

import org.slf4j.LoggerFactory
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.projects.ProjectIO
import java.awt.image.BufferedImage
import qupath.lib.projects.Projects;
import java.io.File
import javax.imageio.ImageIO
import qupath.lib.images.ImageData;
import qupath.lib.gui.commands.ProjectCommands
import javafx.scene.control.Alert
import javafx.stage.Modality
import qupath.lib.projects.Project


class utilityFunctions {

    static void showAlertDialog(String message){
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning!");
        alert.setHeaderText(null);
        alert.setContentText(message);

    // This line makes the alert a modal dialog
        alert.initModality(Modality.APPLICATION_MODAL);

        alert.showAndWait();
    }

    static boolean addImageToProject(File stitchedImagePath, Project project){
        def logger = LoggerFactory.getLogger(QuPathGUI.class)

        def imagePath = stitchedImagePath.toURI().toString()
        //logger.info(imagePath)

        def support = ImageServerProvider.getPreferredUriImageSupport(BufferedImage.class, imagePath)
        //logger.info(support as String)
        def builder = support.builders.get(0)
        // Make sure we don't have null
        if (builder == null) {
            logger.warn("Image not supported: " + imagePath)
            return false
        }

        // Add the image as entry to the project
        logger.info("Adding: " + imagePath)
        if (project == null) {
            logger.warn("Project is null, there must have been a problem creating the project")
            return false
        }
        Object entry = project.addImage(builder)

        // Set a particular image type
        def imageData = entry.readImageData()
        imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
        entry.saveImageData(imageData)

        // Write a thumbnail if we can
        var img = ProjectCommands.getThumbnailRGB(imageData.getServer());
        entry.setThumbnail(img)

        // Add an entry name (the filename)
        entry.setImageName(stitchedImagePath.getName())
        project.syncChanges()
        return true;

}

    static Project createProjectFolder(String projectsFolderPath, String sampleLabel, String scanType) {
        //TODO check if a project is open! It probably should not be?

        // Ensure that the projectsFolderPath exists, if it does not, create it.
        File projectsFolder = new File(projectsFolderPath)
        if (!projectsFolder.exists()) {
            projectsFolder.mkdirs()
        }

        // Within projectsFolderPath, check for a folder named sampleLabel, if it does not exist, create it.
        File sampleLabelFolder = new File(projectsFolder, sampleLabel)
        if (!sampleLabelFolder.exists()) {
            sampleLabelFolder.mkdirs()
        }

        // Check for a .qpproj file in the sampleLabel folder
        File[] qpprojFiles = sampleLabelFolder.listFiles({ dir, name -> name.endsWith('.qpproj') } as FilenameFilter)
        //Create a QuPath project in the sampleLabelFolder, within the projects folder, provided there are no existing .qpproj files
        Project project = null
        if (qpprojFiles == null || qpprojFiles.length == 0) {
            project = Projects.createProject(sampleLabelFolder, BufferedImage.class)
        }else{
            //WARNING: This assumes there will be only one file ending in .qpproj
            // this should USUALLY be a safe assumption
            if (qpprojFiles.length > 1){
                Dialogs.showWarningNotification("Warning!", "Multiple Project files found, may cause unexpected behavior!")
            }

            project = ProjectIO.loadProject(qpprojFiles[0], BufferedImage.class)
        }

        if (project == null) {
            Dialogs.showWarningNotification("Warning!", "Project is null!")
        }
        // Within projectsFolderPath, check for a folder with the name "SlideImages", if it does not exist, create it
        String slideImagesFolderPathStr = projectsFolderPath + File.separator + sampleLabel + File.separator + "SlideImages" ;
        File slideImagesFolder =  new File(slideImagesFolderPathStr);

        if (!slideImagesFolder.exists()) {
            slideImagesFolder.mkdirs()
        }

        // Within projectFolderPath/sampleLabel, check for a folder with the name that combines the two variables scanType+sampleLabel, if it does not exist, create it
        File scanTypeFolder = new File(sampleLabelFolder, scanType + sampleLabel)
        if (!scanTypeFolder.exists()) {
            scanTypeFolder.mkdirs()
        }

        return project
    }

    /**
     * Executes a Python script using a specified Python executable within a virtual environment.
     * This method is designed to be compatible with Windows, Linux, and macOS.
     *
     * @param anacondaEnvPath The path to the Python virtual environment.
     * @param pythonScriptPath The path to the Python script to be executed.
     * @param x1 The first x-coordinate to be passed to the Python script.
     * @param y1 The first y-coordinate to be passed to the Python script.
     * @param x2 The second x-coordinate to be passed to the Python script.
     * @param y2 The second y-coordinate to be passed to the Python script.
     */
    static void runPythonCommand(String anacondaEnvPath, String pythonScriptPath,String projectsFolderPath, String sampleLabel, String x1, String y1, String x2, String y2) {
        try {
            def logger = LoggerFactory.getLogger(QuPathGUI.class)
            // Path to the Python executable in the Anaconda environment
            String pythonExecutable = "${anacondaEnvPath}/python.exe";

            // Combine coordinates into a single argument
            String args = "$pythonScriptPath, $projectsFolderPath, $sampleLabel, ${x1},${y1},${x2},${y2}";
            logger.info(args)
            // Construct the command
            String command = "\"${pythonExecutable}\" \"${pythonScriptPath}\" ${args}";
            logger.info(command)
            // Execute the command
            Process process = command.execute();
            process.waitFor();

            // Read and log standard output
            process.inputStream.eachLine { line ->
                logger.info(line)
            }

            // Read and log standard error
            process.errorStream.eachLine { line ->
                logger.error(line)
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Map<String, String> getPreferences() {
        //TODO add code to access Preferences fields
        //If preferences are null or missing, throw an error and close
        //Open to discussion whether scan types should be included here or typed every time, or some other option
        //TODO fix the installation to be a folder with an expected .py file target? Or keep as .py file target?
        return [installation: "C:\\ImageAnalysis\\python\\py_dummydoc.py", environment: "C:\\Anaconda\\envs\\paquo", projects: "C:\\ImageAnalysis\\slides", firstScanType: "4x_bf_", secondScanType:"20x_bf"]
    }
}