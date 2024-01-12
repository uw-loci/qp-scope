package qupath.ext.qp_scope.utilities

import javafx.scene.control.Alert
import javafx.stage.Modality
import org.slf4j.LoggerFactory
import qupath.lib.gui.commands.ProjectCommands
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.images.ImageData
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.objects.PathObject
import qupath.lib.projects.Project
import qupath.lib.projects.ProjectIO
import qupath.lib.projects.Projects
import qupath.lib.scripting.QP

import java.awt.image.BufferedImage
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream;


class utilityFunctions {

    static final logger = LoggerFactory.getLogger(utilityFunctions.class)

    static void showAlertDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning!");
        alert.setHeaderText(null);
        alert.setContentText(message);

        // This line makes the alert a modal dialog
        alert.initModality(Modality.APPLICATION_MODAL);

        alert.showAndWait();
    }

    static boolean addImageToProject(File stitchedImagePath, Project project) {

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
        } else {
            //WARNING: This assumes there will be only one file ending in .qpproj
            // this should USUALLY be a safe assumption
            if (qpprojFiles.length > 1) {
                Dialogs.showWarningNotification("Warning!", "Multiple Project files found, may cause unexpected behavior!")
            }

            project = ProjectIO.loadProject(qpprojFiles[0], BufferedImage.class)
        }

        if (project == null) {
            Dialogs.showWarningNotification("Warning!", "Project is null!")
        }
        // Within projectsFolderPath, check for a folder with the name "SlideImages", if it does not exist, create it
        String slideImagesFolderPathStr = projectsFolderPath + File.separator + sampleLabel + File.separator + "SlideImages";
        File slideImagesFolder = new File(slideImagesFolderPathStr);

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
     * @param arguments A list of arguments to pass to the python script. The amount may vary, and different scripts will be run depending on the number of arguments passed
     */
    static runPythonCommand(String anacondaEnvPath, String pythonScriptPath, List arguments) {
        try {
            String pythonExecutable = new File(anacondaEnvPath, "python.exe").getCanonicalPath();

            // Adjust the pythonScriptPath based on arguments
            if (arguments == null) {
                // Change the script to 'getStageCoordinates.py'
                File scriptFile = new File(pythonScriptPath);
                pythonScriptPath = new File(scriptFile.getParent(), "getStageCoordinates.py").getCanonicalPath();
                // Construct the command
                String command = "\"" + pythonExecutable + "\" -u \"" + pythonScriptPath + "\" " + arguments;
                // Execute the command
                Process process = command.execute();
                logger.info("Executing command: " + command);
                logger.info("This should get stage coordinates back")
                List<String> result = handleProcessOutput(process)
                if (result != null) {
                    logger.info("Received coordinates: ${result[0]}, ${result[1]}")
                    return result
                } else {
                    logger.error("Error occurred or no valid output received from the script.")
                    return null
                }
            } else if (arguments.size() == 2) {
                // Change the script to 'moveStageToCoordinates.py'
                File scriptFile = new File(pythonScriptPath);
                pythonScriptPath = new File(scriptFile.parent, "moveStageToCoordinates.py").canonicalPath
            }

            String args = arguments != null ? arguments.collect { "\"$it\"" }.join(' ') : "";

            // Construct the command
            String command = "\"" + pythonExecutable + "\" -u \"" + pythonScriptPath + "\" " + args;
            logger.info("Executing command: " + command);

            // Execute the command
            Process process = command.execute();

            // Redirect the output and error streams to the logger
            process.consumeProcessOutput(new StringWriter(), new StringWriter())

            // Wait for the process to complete
            process.waitFor()

            // Log the output and error (or use it as needed)
            logger.info(process.text) // This logs the standard output
            logger.error(process.err.text) // This logs the standard error
            return null
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static List<String> handleProcessOutput(Process process) {
        BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        String line;
        List<String> outputLines = []
        List<String> errorLines = []
        String value1 = null
        String value2 = null

        while ((line = outputReader.readLine()) != null) {
            outputLines.add(line)
            // Assuming coordinates are on the first line
            if (outputLines.size() == 1) {
                String[] values = line.split(" ");
                value1 = values[0];
                value2 = values[1];
            }
        }

        while ((line = errorReader.readLine()) != null) {
            errorLines.add(line)
        }

        // Check for errors or invalid output
        if (!errorLines.isEmpty() || value1 == null || value2 == null) {
            return null;
        }

        return [value1, value2];
    }

//    static void runPythonCommand(String anacondaEnvPath, String pythonScriptPath, List arguments) {
//        try {
//            def logger = LoggerFactory.getLogger(QuPathGUI.class)
//            String pythonExecutable = new File(anacondaEnvPath, "python.exe").getCanonicalPath()
//
//
//
//            String args = arguments.collect { "\"$it\"" }.join(' ')
//
//            // Construct the command
//            String command = "\"${pythonExecutable}\" -u \"${pythonScriptPath}\" ${args}"
//            logger.info("Executing command: ${command}")
//
//            // Execute the command
//            Process process = command.execute()
//            process.waitFor()
//
//            // Read and log standard output
//            process.inputStream.eachLine { line -> logger.info(line) }
//
//            // Read and log standard error
//            process.errorStream.eachLine { line -> logger.error(line) }
//        } catch (Exception e) {
//            e.printStackTrace()
//        }
//    }


    static Map<String, String> getPreferences() {
        //TODO add code to access Preferences fields
        //If preferences are null or missing, throw an error and close
        //Open to discussion whether scan types should be included here or typed every time, or some other option
        //TODO fix the installation to be a folder with an expected .py file target? Or keep as .py file target?
        return [ pycromanager   : "C:\\ImageAnalysis\\QPExtensionTest\\qp_scope\\src\\main\\pythonScripts/4x_bf_scan_pycromanager.py",
                environment    : "C:\\Anaconda\\envs\\paquo",
                projects       : "C:\\ImageAnalysis\\QPExtensionTest\\data\\slides",
                tissueDetection: "DetectTissue.groovy",
                firstScanType  : "4x_bf",
                secondScanType : "20x_bf",
                tileHandling   : "Zip"] //Zip Delete or anything else is ignored
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
            folder.mkdirs();
            // This will create the directory including any necessary but nonexistent parent directories.
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

    static String transformBoundingBox(double x1, double y1, double x2, double y2, String pixelSize, String xCoordinate, String yCoordinate, boolean flip) {
        if (flip) {
            logger.info("Handling flip")
        }
        // Log the values of all input parameters
        logger.info("Input Parameters - x1: $x1, y1: $y1, x2: $x2, y2: $y2, pixelSize: $pixelSize, xCoordinate: $xCoordinate, yCoordinate: $yCoordinate, flip: $flip")

        double pixelSizeDouble = parseDoubleSafely(pixelSize)
        double xCoordinateDouble = parseDoubleSafely(xCoordinate)
        double yCoordinateDouble = parseDoubleSafely(yCoordinate)

        // Convert pixel coordinates to microns
        double x1Microns = x1 * pixelSizeDouble
        double y1Microns = y1 * pixelSizeDouble
        double x2Microns = x2 * pixelSizeDouble
        double y2Microns = y2 * pixelSizeDouble

        // Adjust coordinates relative to the upper right coordinates
        double adjustedX1 = xCoordinateDouble - x1Microns
        double adjustedY1 = yCoordinateDouble - y1Microns
        double adjustedX2 = xCoordinateDouble - x2Microns
        double adjustedY2 = yCoordinateDouble - y2Microns

        // Create the bounding box string in the format "x1, y1, x2, y2"
        String boundingBox = "$adjustedX1, $adjustedY1, $adjustedX2, $adjustedY2"
        return boundingBox
    }

    static double parseDoubleSafely(String str) {
        try {
            return str?.trim()?.toDouble() ?: 0.0
        } catch (NumberFormatException e) {
            logger.error("NumberFormatException in parsing string to double: ${e.message}")
            return 0.0
        }
    }


    /**
     * Modifies the specified Groovy script by updating the pixel size and the JSON file path.
     *
     * @param groovyScriptPath The path to the Groovy script file.
     * @param pixelSize The new pixel size to set in the script.
     * @param jsonFilePathString The new JSON file path to set in the script.
     * @throws IOException if an I/O error occurs reading from or writing to the file.
     */
    public static String modifyTissueDetectScript(String groovyScriptPath, String pixelSize, String jsonFilePathString) throws IOException {
        // Read, modify, and write the script in one go
        List<String> lines = Files.lines(Paths.get(groovyScriptPath), StandardCharsets.UTF_8)
                .map(line -> {
                    if (line.startsWith("setPixelSizeMicrons")) {
                        return "setPixelSizeMicrons(" + pixelSize + ", " + pixelSize + ")";
                    } else if (line.startsWith("createAnnotationsFromPixelClassifier")) {
                        return line.replaceFirst("\"[^\"]*\"", "\"" + jsonFilePathString + "\"");
                    } else {
                        return line;
                    }
                })
                .collect(Collectors.toList());

        return String.join(System.lineSeparator(), lines);
    }

    /**
     * Modifies the specified export script by updating the pixel size source and the base directory, and returns the modified script as a string.
     *
     * @param exportScriptPathString The path to the export script file.
     * @param pixelSize The new pixel size to set in the script.
     * @param tilesCSVdirectory The new base directory to set in the script.
     * @return String representing the modified script.
     * @throws IOException if an I/O error occurs reading from the file.
     */
    public static String modifyCSVExportScript(String exportScriptPathString, String pixelSize, String tilesCSVdirectory) throws IOException {
        // Read and modify the script
        List<String> lines = Files.lines(Paths.get(exportScriptPathString), StandardCharsets.UTF_8)
                .map(line -> {
                    if (line.startsWith("double pixelSizeSource")) {
                        return "double pixelSizeSource = " + pixelSize + ";";
                    } else if (line.startsWith("baseDirectory")) {
                        return "baseDirectory = \"" + tilesCSVdirectory.replace("\\", "\\\\") + "\";";
                    } else {
                        return line;
                    }
                })
                .collect(Collectors.toList());

        // Join the lines into a single string
        return String.join(System.lineSeparator(), lines);
    }

    /**
     * Extracts the file path from the server path string.
     *
     * @param serverPath The server path string.
     * @return The extracted file path, or null if the path could not be extracted.
     */
    public static String extractFilePath(String serverPath) {
        // Regular expression to match the file path
        String regex = "file:/(.*?\\.TIF)";

        // Create a pattern and matcher for the regular expression
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(serverPath);

        // Check if the pattern matches and return the file path
        if (matcher.find()) {
            return matcher.group(1).replaceFirst("^/", "").replaceAll("%20", " ");
        } else {
            return null; // No match found
        }
    }

    static List<Object> getTopCenterTile(Collection<PathObject> detections) {
        // Filter out null detections and sort by Y-coordinate
        List<PathObject> sortedDetections = detections.findAll { it != null }
                .sort { it.getROI().getCentroidY() }

        // Get the minimum Y-coordinate (top tiles)
        double minY = sortedDetections.first().getROI().getCentroidY()

        // Get all tiles that are at the top
        List<PathObject> topTiles = sortedDetections.findAll { it.getROI().getCentroidY() == minY }

        // Find the median X-coordinate of the top tiles
        List<Double> xCoordinates = topTiles.collect { it.getROI().getCentroidX() }
        double medianX = xCoordinates.sort()[xCoordinates.size() / 2]

        // Select the top tile closest to the median X-coordinate
        PathObject topCenterTile = topTiles.min { Math.abs(it.getROI().getCentroidX() - medianX) }

        return [topCenterTile.getROI().getCentroidX(), topCenterTile.getROI().getCentroidY(), topCenterTile]
    }

    static List<Object> getLeftCenterTile(Collection<PathObject> detections) {
        // Filter out null detections and sort by X-coordinate
        List<PathObject> sortedDetections = detections.findAll { it != null }
                .sort { it.getROI().getCentroidX() }

        // Get the minimum X-coordinate (left tiles)
        double minX = sortedDetections.first().getROI().getCentroidX()

        // Get all tiles that are at the left
        List<PathObject> leftTiles = sortedDetections.findAll { it.getROI().getCentroidX() == minX }

        // Find the median Y-coordinate of the left tiles
        List<Double> yCoordinates = leftTiles.collect { it.getROI().getCentroidY() }
        double medianY = yCoordinates.sort()[yCoordinates.size() / 2]

        // Select the left tile closest to the median Y-coordinate
        PathObject leftCenterTile = leftTiles.min { Math.abs(it.getROI().getCentroidY() - medianY) }

        return [leftCenterTile.getROI().getCentroidX(), leftCenterTile.getROI().getCentroidY(), leftCenterTile]
    }

    //TODO possibly use QuPath's affine transformation tools
    //Convert the QuPath pixel based coordinates for a location into the MicroManager micron based stage coordinates
    static List<Double> QPtoMicroscopeCoordinates(List<Double> qpCoordinates, Double imagePixelSize, Object transformation) {
        //TODO figure out conversion
        def xUpperLeft = qpCoordinates[0]*imagePixelSize
        def yUpperLeft = qpCoordinates[1]*imagePixelSize


        def mmCoordinates = qpCoordinates
        return mmCoordinates
    }


    static List<Double> updateTransformation(List<Double> transformation, List<String> coordinatesQP, List<String> coordinatesMM) {
        logger.info("Transformation input: $transformation (Type: ${transformation.getClass()})")
        logger.info("Coordinates QP input: $coordinatesQP (Type: ${coordinatesQP.getClass()})")
        logger.info("Coordinates MM input: $coordinatesMM (Type: ${coordinatesMM.getClass()})")

        // Extract transformation elements
        double xShiftMicrons = transformation[0]
        double yShiftMicrons = transformation[1]
        double pixelSize = transformation[2]

        logger.info("Extracted xShiftMicrons: $xShiftMicrons")
        logger.info("Extracted yShiftMicrons: $yShiftMicrons")
        logger.info("Extracted pixelSize: $pixelSize")

        // Convert coordinatesQP and coordinatesMM elements from String to Double
        double xQP = coordinatesQP[0].toDouble()
        double yQP = coordinatesQP[1].toDouble()
        double xMM = coordinatesMM[0].toDouble()
        double yMM = coordinatesMM[1].toDouble()

        logger.info("Converted xQP from String to Double: $xQP")
        logger.info("Converted yQP from String to Double: $yQP")
        logger.info("Converted xMM from String to Double: $xMM")
        logger.info("Converted yMM from String to Double: $yMM")

        // Calculate coordinate shift
        double xShift = xQP * pixelSize - xMM
        double yShift = yQP * pixelSize - yMM

        logger.info("Calculated xShift: $xShift")
        logger.info("Calculated yShift: $yShift")

        // Update transformation values
        transformation[0] = xShiftMicrons - xShift
        transformation[1] = yShiftMicrons - yShift

        logger.info("Updated transformation: $transformation")

        return transformation
    }
}