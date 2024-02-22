package qupath.ext.qp_scope.utilities

import javafx.scene.control.Alert
import javafx.stage.Modality
import org.slf4j.LoggerFactory
import groovy.json.JsonSlurper
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern


class MinorFunctions {
    static final logger = LoggerFactory.getLogger(MinorFunctions.class)

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
     * Reads a YAML file and parses it into a Groovy map or list structure.
     * This method uses the SnakeYAML library to parse the YAML content,
     * enabling the handling of complex, nested YAML structures.
     *
     * @param filePath The path to the YAML file to be read and parsed.
     * @return A map or list representation of the YAML file's content,
     *         allowing for multi-layered maps if the YAML is nested.
     */
    def readYamlFileToMap(String filePath) {
        Yaml yaml = new Yaml()
        String content = new String(Files.readAllBytes(Paths.get(filePath)))
        def data = yaml.load(content)
        return data
    }

    /**
     * Reads a JSON file and parses it into a Groovy map or list structure.
     * The structure of the returned object depends on the JSON content,
     * allowing for multi-layered maps if the JSON is nested.
     *
     * @param filePath The path to the JSON file to be read and parsed.
     * @return A map or list representation of the JSON file's content,
     *         depending on the JSON structure.
     */
    def readJsonFileToMap(String filePath) {
        def jsonSlurper = new JsonSlurper()
        def file = new File(filePath)
        def data = jsonSlurper.parse(file)
        return data
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

}
