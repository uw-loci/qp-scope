package qupath.ext.qp_scope.utilities

import org.slf4j.LoggerFactory
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.projects.ProjectIO
import qupath.lib.scripting.QP

import java.awt.image.BufferedImage
import qupath.lib.projects.Projects;
import java.io.File
import javax.imageio.ImageIO
import qupath.lib.images.ImageData;
import qupath.lib.gui.commands.ProjectCommands
import javafx.scene.control.Alert
import javafx.stage.Modality
import qupath.lib.projects.Project

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;


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
    static void runPythonCommand(String anacondaEnvPath, String pythonScriptPath, List arguments) {
        try {
            def logger = LoggerFactory.getLogger(QuPathGUI.class)
            String pythonExecutable = new File(anacondaEnvPath, "python.exe").getCanonicalPath()

//            List<String> arguments = [pythonScriptPath, projectsFolderPath, sampleLabel, imageType]
//            if (x1) arguments.add(x1)
//            if (y1) arguments.add(y1)
//            if (x2) arguments.add(x2)
//            if (y2) arguments.add(y2)
//            if (annotationJsonFileLocation) arguments.add(annotationJsonFileLocation)

            String args = arguments.collect { "\"$it\"" }.join(' ')

            // Construct the command
            String command = "\"${pythonExecutable}\" -u \"${pythonScriptPath}\" ${args}"
            logger.info("Executing command: ${command}")

            // Execute the command
            Process process = command.execute()
            process.waitFor()

            // Read and log standard output
            process.inputStream.eachLine { line -> logger.info(line) }

            // Read and log standard error
            process.errorStream.eachLine { line -> logger.error(line) }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }


    static Map<String, String> getPreferences() {
        //TODO add code to access Preferences fields
        //If preferences are null or missing, throw an error and close
        //Open to discussion whether scan types should be included here or typed every time, or some other option
        //TODO fix the installation to be a folder with an expected .py file target? Or keep as .py file target?
        return [installation: "C:\\ImageAnalysis\\python\\pycromanager_step_1.py",
                environment: "C:\\Anaconda\\envs\\paquo",
                projects: "C:\\ImageAnalysis\\slides",
                firstScanType: "4x_bf",
                secondScanType:"20x_bf",
                tileHandling:"Zip"] //Zip Delete or anything else is ignored
    }
/**
 * Exports all annotations to a JSON file in the specified JSON subfolder of the current project.
 *
 * @param projectsFolderPath The base path to the projects folder.
 * @param sampleLabel The label of the sample.
 * @param firstScanType The type of the first scan.
 */
    static String createAnnotationJson(String projectsFolderPath, String sampleLabel, String firstScanType) {
        // Construct the folder path for storing the JSON file
        File folder = new File(projectsFolderPath + File.separator + sampleLabel + File.separator + "JSON");

        // Check if the folder exists, and create it if it doesn't
        if (!folder.exists()) {
            folder.mkdirs();  // This will create the directory including any necessary but nonexistent parent directories.
        }

        // Construct the full path for the annotation JSON file
        File annotationJsonFile = new File(folder, firstScanType + sampleLabel + ".geojson");
        String annotationJsonFileLocation = annotationJsonFile.getPath();

        // Export all annotations to the GeoJSON file
        QP.exportAllObjectsToGeoJson(annotationJsonFileLocation, "EXCLUDE_MEASUREMENTS", "FEATURE_COLLECTION");

        return annotationJsonFileLocation
    }
/**
 * Generates a unique folder name by checking the number of existing folders with a similar name
 * in the current directory, and then appending that number to the folder name.
 * The naming starts with _1 and increments for each additional folder with a similar base name.
 *
 * @param originalFolderPath The original folder path.
 * @return A unique folder name.
 */
    static String getUniqueFolderName(String originalFolderPath) {
        Path path = Paths.get(originalFolderPath);
        Path parentDir = path.getParent();
        String baseName = path.getFileName().toString();

        int counter = 1;
        Path newPath = parentDir.resolve(baseName + "_" + counter);

        // Check for existing folders with the same base name and increment counter
        while (Files.exists(newPath)) {
            counter++;
            newPath = parentDir.resolve(baseName + "_" + counter);
        }

        // Return only the unique folder name, not the full path
        return newPath.getFileName().toString();
    }

    /**
     * Deletes all the tiles within the provided folder and the folder itself.
     *
     * @param folderPath The path to the folder containing the tiles to be deleted.
     */
    public static void deleteTilesAndFolder(String folderPath) {
        def logger = LoggerFactory.getLogger(QuPathGUI.class)
        try {

            Path directory = Paths.get(folderPath)

            // Delete all files in the folder
            Files.walk(directory)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.delete(path)
                        } catch (IOException ex) {
                            logger.error("Error deleting file: " + path, ex)
                        }
                    })

            // Delete the folder itself
            Files.delete(directory)
        } catch (IOException ex) {
            logger.error("Error deleting folder: " + folderPath, ex)
        }
    }

    public static void zipTilesAndMove(String folderPath) {
        def logger = LoggerFactory.getLogger(QuPathGUI.class)
        try {
            Path directory = Paths.get(folderPath);
            Path parentDirectory = directory.getParent();
            Path compressedTilesDir = parentDirectory.resolve("Compressed tiles");

            // Create "Compressed tiles" directory if it doesn't exist
            if (!Files.exists(compressedTilesDir)) {
                Files.createDirectory(compressedTilesDir);
            }

            // Create a Zip file
            String zipFileName = directory.getFileName().toString() + ".zip";
            Path zipFilePath = compressedTilesDir.resolve(zipFileName);
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
                Files.walk(directory)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            ZipEntry zipEntry = new ZipEntry(directory.relativize(path).toString());
                            try {
                                zos.putNextEntry(zipEntry);
                                Files.copy(path, zos);
                                zos.closeEntry();
                            } catch (IOException ex) {
                                logger.error("Error adding file to zip: " + path, ex);
                            }
                        });
            }

            // Optionally, delete the original tiles and folder
            // deleteTilesAndFolder(folderPath);
        } catch (IOException ex) {
            logger.error("Error zipping and moving tiles from: " + folderPath, ex);
        }
    }

}