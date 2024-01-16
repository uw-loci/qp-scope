package qupath.ext.qp_scope.utilities


import org.slf4j.LoggerFactory
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.commands.ProjectCommands
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.images.ImageData
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.projects.Project
import qupath.lib.projects.ProjectIO
import qupath.lib.projects.Projects
import qupath.ext.basicstitching.stitching.StitchingImplementations

import java.awt.image.BufferedImage
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class UtilityFunctions {

    static final logger = LoggerFactory.getLogger(UtilityFunctions.class)


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
        var img = ProjectCommands.getThumbnailRGB(imageData.getServer())
        entry.setThumbnail(img)

        // Add an entry name (the filename)
        entry.setImageName(stitchedImagePath.getName())
        project.syncChanges()
        return true

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
        String slideImagesFolderPathStr = projectsFolderPath + File.separator + sampleLabel + File.separator + "SlideImages"
        File slideImagesFolder = new File(slideImagesFolderPathStr)

        if (!slideImagesFolder.exists()) {
            slideImagesFolder.mkdirs()
        }

        return project
    }

    /**
     * Performs image stitching and updates the QuPath project with the stitched image.
     *
     * @param stitchingImplementations An instance or reference to the stitching implementations.
     * @param projectsFolderPath The path to the projects folder.
     * @param sampleLabel The label for the sample.
     * @param scanTypeWithIndex The scan type with an appended index for uniqueness.
     * @param qupathGUI The QuPath GUI instance for updating the project.
     * @param currentQuPathProject The current QuPath project to be updated.
     * @return The path to the stitched image.
     */
    static String stitchImagesAndUpdateProject(StitchingImplementations stitchingImplementations,
                                                       String projectsFolderPath, String sampleLabel,
                                                       String scanTypeWithIndex, QuPathGUI qupathGUI,
                                                       Project currentQuPathProject) {

        String stitchedImageOutputFolder = projectsFolderPath + File.separator + sampleLabel + File.separator + "SlideImages"
        //TODO Need to check if stitching is successful, provide error
        String stitchedImagePathStr = stitchingImplementations.stitchCore("Coordinates in TileConfiguration.txt file",
                projectsFolderPath + File.separator + sampleLabel,
                stitchedImageOutputFolder, "J2K_LOSSY",
                0, 1, scanTypeWithIndex)
        File stitchedImagePath = new File(stitchedImagePathStr)
        addImageToProject(stitchedImagePath, currentQuPathProject)

        qupathGUI.setProject(currentQuPathProject)
        qupathGUI.refreshProject()

        return stitchedImagePathStr
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
            String pythonExecutable = new File(anacondaEnvPath, "python.exe").getCanonicalPath()

            // Adjust the pythonScriptPath based on arguments
            if (arguments == null) {
                // Change the script to 'getStageCoordinates.py'
                File scriptFile = new File(pythonScriptPath)
                pythonScriptPath = new File(scriptFile.getParent(), "getStageCoordinates.py").getCanonicalPath()
                // Construct the command
                String command = "\"" + pythonExecutable + "\" -u \"" + pythonScriptPath + "\" " + arguments
                // Execute the command
                Process process = command.execute()
                logger.info("Executing command: " + command)
                logger.info("This should get stage coordinates back")
                List<String> result = handleProcessOutput(process)
                if (result != null) {
                    logger.info("Received output: ${result.join(', ')}")
                    return result
                } else {
                    logger.error("Error occurred or no valid output received from the script.")
                    return null
                }
            } else if (arguments.size() == 2) {
                // Change the script to 'moveStageToCoordinates.py'
                File scriptFile = new File(pythonScriptPath)
                pythonScriptPath = new File(scriptFile.parent, "moveStageToCoordinates.py").canonicalPath
            }

            String args = arguments != null ? arguments.collect { "\"$it\"" }.join(' ') : ""

            // Construct the command
            String command = "\"" + pythonExecutable + "\" -u \"" + pythonScriptPath + "\" " + args
            logger.info("Executing command: " + command)

            // Execute the command
            Process process = command.execute()

            // Redirect the output and error streams to the logger
            process.consumeProcessOutput(new StringWriter(), new StringWriter())

            // Wait for the process to complete
            process.waitFor()

            // Log the output and error (or use it as needed)
            logger.info(process.text) // This logs the standard output
            logger.error(process.err.text) // This logs the standard error
            return null
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    static List<String> handleProcessOutput(Process process) {
        BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))

        String line
        List<String> outputLines = []
        List<String> errorLines = []
        String value1 = null
        String value2 = null

        while ((line = outputReader.readLine()) != null) {
            outputLines.add(line)
            // Assuming coordinates are on the first line
            if (outputLines.size() == 1) {
                String[] values = line.split(" ")
                value1 = values[0]
                value2 = values[1]
            }
        }

        while ((line = errorReader.readLine()) != null) {
            errorLines.add(line)
        }

        // Check for errors or invalid output
        if (!errorLines.isEmpty() || value1 == null || value2 == null) {
            return null
        }

        return [value1, value2]
    }




    static Map<String, String> getPreferences() {
        //TODO add code to access Preferences fields
        //If preferences are null or missing, throw an error and close
        //Open to discussion whether scan types should be included here or typed every time, or some other option
        //TODO fix the installation to be a folder with an expected .py file target? Or keep as .py file target?
        return [pycromanager           : "C:\\ImageAnalysis\\QPExtensionTest\\qp_scope\\src\\main\\pythonScripts/4x_bf_scan_pycromanager.py",
                environment            : "C:\\Anaconda\\envs\\paquo",
                projects               : "C:\\ImageAnalysis\\QPExtensionTest\\data\\slides",
                tissueDetection        : "DetectTissue.groovy",
                firstScanType          : "4x_bf",
                secondScanType         : "20x_bf",
                tileHandling           : "Zip",
                pixelSizeSource        : "7.2",
                pixelSizeFirstScanType : "1.105",
                pixelSizeSecondScanType: "0.5",
                frameWidth             : "1392",
                frameHeight            : "1040",
                overlapPercent         : "0"] //Zip Delete or anything else is ignored
    }


    /**
     * Deletes all the tiles within the provided folder and the folder itself.
     *
     * @param folderPath The path to the folder containing the tiles to be deleted.
     */
    static void deleteTilesAndFolder(String folderPath) {

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

    static void zipTilesAndMove(String folderPath) {

        try {
            Path directory = Paths.get(folderPath)
            Path parentDirectory = directory.getParent()
            Path compressedTilesDir = parentDirectory.resolve("Compressed tiles")

            // Create "Compressed tiles" directory if it doesn't exist
            if (!Files.exists(compressedTilesDir)) {
                Files.createDirectory(compressedTilesDir)
            }

            // Create a Zip file
            String zipFileName = directory.getFileName().toString() + ".zip"
            Path zipFilePath = compressedTilesDir.resolve(zipFileName)
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
                Files.walk(directory)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            ZipEntry zipEntry = new ZipEntry(directory.relativize(path).toString())
                            try {
                                zos.putNextEntry(zipEntry)
                                Files.copy(path, zos)
                                zos.closeEntry()
                            } catch (IOException ex) {
                                logger.error("Error adding file to zip: " + path, ex)
                            }
                        })
            }

            // Optionally, delete the original tiles and folder
            // deleteTilesAndFolder(folderPath);
        } catch (IOException ex) {
            logger.error("Error zipping and moving tiles from: " + folderPath, ex)
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
    static String modifyTissueDetectScript(String groovyScriptPath, String pixelSize, String jsonFilePathString) throws IOException {
        // Read, modify, and write the script in one go
        List<String> lines = Files.lines(Paths.get(groovyScriptPath), StandardCharsets.UTF_8)
                .map(line -> {
                    if (line.startsWith("setPixelSizeMicrons")) {
                        return "setPixelSizeMicrons(" + pixelSize + ", " + pixelSize + ")"
                    } else if (line.startsWith("createAnnotationsFromPixelClassifier")) {
                        return line.replaceFirst("\"[^\"]*\"", "\"" + jsonFilePathString + "\"")
                    } else {
                        return line
                    }
                })
                .collect(Collectors.toList())

        return String.join(System.lineSeparator(), lines)
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
    static String modifyTXTExportScript(String exportScriptPathString, String pixelSize, Map<String, String> preferences, String sampleLabel) throws IOException {
        // Access necessary folder locations to ensure Groovy script saves files correctly
        String baseDirectoryPath = "${preferences.projects}${File.separator}${sampleLabel}".replace("\\", "\\\\")
        // Handle backslashes for Windows paths
        String imagingBasePath = "${baseDirectoryPath}${File.separator}${preferences.firstScanType}"

        String uniqueFolderName = MinorFunctions.getUniqueFolderName(imagingBasePath)

        // Extract the numeric part from the folder name using a regex pattern
        Pattern pattern = Pattern.compile('(\\d+)$') // Using single quotes for regex
        Matcher matcher = pattern.matcher(uniqueFolderName)
        String numericPart = matcher.find() ? matcher.group(1) : '1' // Default to '1' if no numeric part is found


        String imagingModalityValue = "${preferences.firstScanType}_${numericPart}"

        List<String> lines = Files.lines(Paths.get(exportScriptPathString), StandardCharsets.UTF_8)
                .map(line -> {
                    if (line.startsWith("double pixelSizeSource")) {
                        return "double pixelSizeSource = " + pixelSize + ";"
                    } else if (line.startsWith("double pixelSizeTarget")) {
                        return "double pixelSizeTarget = " + preferences.pixelSizeFirstScanType + ";"
                    } else if (line.startsWith("double frameWidth")) {
                        double frameWidth = Double.parseDouble(preferences.frameWidth) / Double.parseDouble(pixelSize) * Double.parseDouble(preferences.pixelSizeFirstScanType)
                        return "double frameWidth = " + frameWidth + ";"
                    } else if (line.startsWith("double frameHeight")) {
                        double frameHeight = Double.parseDouble(preferences.frameHeight) / Double.parseDouble(pixelSize) * Double.parseDouble(preferences.pixelSizeFirstScanType)
                        return "double frameHeight = " + frameHeight + ";"
                    } else if (line.startsWith("double overlapPercent")) {
                        return "double overlapPercent = " + preferences.overlapPercent + ";"
                    } else if (line.startsWith("baseDirectory")) {

                        String newLine = "baseDirectory = \"" + baseDirectoryPath + "\";"
                        logger.info("Replacing baseDirectory line with: " + newLine)
                        return newLine
                    } else if (line.startsWith("imagingModality")) {

                        return "imagingModality = \"" + imagingModalityValue + "\";"
                    } else {
                        return line
                    }
                })
                .collect(Collectors.toList())

        // Join the lines into a single string
        return String.join(System.lineSeparator(), lines)
    }


    /**
     * Modifies a Groovy script content by setting the 'createTiles' variable to false and updating
     * the 'boundingBoxStageCoordinates_um' variable with provided bounding box values.
     *
     * @param scriptContent The content of the script to be modified as a multi-line string.
     * @param boundingBox A list containing the bounding box coordinates (x1, y1, x2, y2).
     * @return A string representing the modified script content.
     */
    static String boundingBoxReadyTXT(String scriptContent, List boundingBox) {
        // Convert bounding box list to a string
        String boundingBoxStr = boundingBox.join(", ")

        // Split the script content into lines
        List<String> lines = scriptContent.split(System.lineSeparator())

        // Modify the script lines
        List<String> modifiedLines = lines.stream()
                .map(line -> {
                    if (line.trim().startsWith("createTiles")) {
                        return "createTiles = false"
                    } else if (line.trim().startsWith("boundingBoxStageCoordinates_um")) {
                        return "boundingBoxStageCoordinates_um = [" + boundingBoxStr + "]"
                    } else {
                        return line
                    }
                })
                .collect(Collectors.toList())

        // Join the modified lines into a single string
        return String.join(System.lineSeparator(), modifiedLines)
    }

}