package qupath.ext.qp_scope.utilities

import javafx.scene.control.Alert
import javafx.stage.Modality
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

class minorFunctions {
    static final logger = LoggerFactory.getLogger(minorFunctions.class)

    static void showAlertDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING)
        alert.setTitle("Warning!")
        alert.setHeaderText(null)
        alert.setContentText(message)

        // This line makes the alert a modal dialog
        alert.initModality(Modality.APPLICATION_MODAL)

        alert.showAndWait()
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
        Path path = Paths.get(originalFolderPath)
        Path parentDir = path.getParent()
        String baseName = path.getFileName().toString()

        int counter = 1
        Path newPath = parentDir.resolve(baseName + "_" + counter)

        // Check for existing folders with the same base name and increment counter
        while (Files.exists(newPath)) {
            counter++
            newPath = parentDir.resolve(baseName + "_" + counter)
        }

        // Return only the unique folder name, not the full path
        return newPath.getFileName().toString()
    }

    private static int getNextImagingModalityIndex(String baseDirectoryPath, String firstScanType) {
        File directory = new File(baseDirectoryPath)
        if (!directory.exists() || !directory.isDirectory()) {
            return 1 // If directory doesn't exist or isn't a directory, start with index 1
        }

        // Filter directories that match the pattern and find the highest index
        int maxIndex = Arrays.stream(directory.listFiles())
                .filter(File::isDirectory)
                .map(File::getName)
                .filter(name -> name.startsWith(firstScanType + "_"))
                .map(name -> {
                    try {
                        return Integer.parseInt(name.substring(name.lastIndexOf('_') + 1))
                    } catch (NumberFormatException e) {
                        return 0 // If the part after '_' is not a number, return 0
                    }
                })
                .max(Integer::compare)
                .orElse(0) // If no matching directories, start with index 1

        return maxIndex + 1 // Increment the index for the next modality
    }
    /**
     * Extracts the file path from the server path string.
     *
     * @param serverPath The server path string.
     * @return The extracted file path, or null if the path could not be extracted.
     */
    static String extractFilePath(String serverPath) {
        // Regular expression to match the file path
        String regex = "file:/(.*?\\.TIF)"

        // Create a pattern and matcher for the regular expression
        Pattern pattern = Pattern.compile(regex)
        Matcher matcher = pattern.matcher(serverPath)

        // Check if the pattern matches and return the file path
        if (matcher.find()) {
            return matcher.group(1).replaceFirst("^/", "").replaceAll("%20", " ")
        } else {
            return null // No match found
        }
    }
    static double parseDoubleSafely(String str) {
        try {
            return str?.trim()?.toDouble() ?: 0.0
        } catch (NumberFormatException e) {
            logger.error("NumberFormatException in parsing string to double: ${e.message}")
            return 0.0
        }
    }

}
