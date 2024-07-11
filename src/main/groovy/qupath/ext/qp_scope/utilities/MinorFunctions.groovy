package qupath.ext.qp_scope.utilities

import javafx.scene.control.Alert
import javafx.stage.Modality
import org.slf4j.LoggerFactory
import com.google.gson.Gson
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.regex.Matcher
import java.util.regex.Pattern
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

class MinorFunctions {
    static final logger = LoggerFactory.getLogger(MinorFunctions.class)


    /**
     * Attempts to convert each element of a list to a Double.
     * Non-convertible elements are skipped.
     *
     * @param list The list to convert.
     * @return A new list of Double values.
     */
    static List<Double> convertListToDouble(List<?> list) {
        List<Double> doubleList = []
        list.each {
            try {
                doubleList.add(it.toString().toDouble())
            } catch (NumberFormatException e) {
                // Log the error or handle it as needed
                println("Warning: Skipping unconvertible element '$it'")
                // Optionally, add a default value instead of skipping
                // doubleList.add(0.0)
            }
        }
        return doubleList
    }

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
    static def readYamlFileToMap(String filePath) {
        Yaml yaml = new Yaml()
        String content = new String(Files.readAllBytes(Paths.get(filePath)))
        def data = yaml.load(content)
        return data
    }

/**
 * Reads a JSON file and parses it into a multi-layered map using Gson.
 *
 * @param filePath The path to the JSON file to be read and parsed.
 * @return A map representation of the JSON file's content.
 * @throws IOException If an I/O error occurs reading from the file or a malformed or unmappable byte sequence is read.
 */
    public static Map<String, Object> readJsonFileToMapWithGson(String filePath) throws IOException {
        // Read the entire file content into a String
        String content = new String(Files.readAllBytes(Paths.get(filePath)));

        // Use Gson to parse the string into a map
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> data = gson.fromJson(content, type);

        return data;
    }



    /**
     * Counts the number of .tif entries in a TileConfiguration.txt file located in a directory constructed from a list of arguments.
     *
     * @param arguments A list containing the base path to the projects folder, sample label, scan type with index, and an optional subfolder name.
     * @return The count of .tif entries in the TileConfiguration.txt file, or 0 if the file does not exist.
     */
    static int countTifEntriesInTileConfig(List<String> arguments) {
        // Construct the path to the TileConfiguration.txt file
        String tileConfigPath = arguments.join("/") + "/TileConfiguration_QP.txt"

        File tileConfigFile = new File(tileConfigPath)

        // Check if the TileConfiguration.txt or TileConfiguration_QP.txt file exists
        if (!tileConfigFile.exists()) {
            tileConfigPath = arguments.join("/") + "/TileConfiguration.txt"
            tileConfigFile = new File(tileConfigPath)
        } else { logger.info("Reading file at $tileConfigPath")}

        if (!tileConfigFile.exists()) {
            println "TileConfiguration file not found at: $tileConfigPath"
            return 0
        } else { logger.info("Reading file at $tileConfigPath")}
        // Count .tif entries in the file
        int tifCount = 0
        int totalLines = 0
        tileConfigFile.eachLine { line ->
            totalLines++
            if (line.contains(".tif")) {
                //logger.info("Current count: $tifCount")
                //logger.info("line is $line")
                tifCount++
            }
        }

        logger.info( "Found $tifCount .tif entries in TileConfiguration.txt out of $totalLines")
        return tifCount
    }

    public static Map<String, String> calculateScriptPaths(String groovyScriptPath) {
        Path groovyScriptDirectory = Paths.get(groovyScriptPath).getParent()
        groovyScriptDirectory = groovyScriptDirectory.resolveSibling("groovyScripts")
        //TODO THIS CANNOT BE HARD CODED
        Path jsonTissueClassfierPath = groovyScriptDirectory.resolve("TissueCAMM.json")
        Path exportScriptPath = groovyScriptDirectory.resolve("save4xMacroTiling.groovy")

        return [
                jsonTissueClassfierPathString: jsonTissueClassfierPath.toString().replace("\\", "/"),
                exportScriptPathString: exportScriptPath.toString().replace("\\", "/")
        ]
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
