package qupath.ext.qp_scope.utilities

import org.slf4j.LoggerFactory
import qupath.lib.gui.QuPathGUI
import qupath.lib.images.writers.ome.OMEPyramidWriter

import javax.imageio.ImageIO

class utilityFunctions {
    static OMEPyramidWriter.CompressionType getCompressionType(String selectedOption) {
        switch (selectedOption) {
            case "Lossy compression":
                return OMEPyramidWriter.CompressionType.J2K_LOSSY
            case "Lossless compression":
                return OMEPyramidWriter.CompressionType.J2K
            default:
                return null // or a default value
        }
    }

    static Map<String, Integer> getTiffDimensions(File filePath) {
        def logger = LoggerFactory.getLogger(QuPathGUI.class)
        if (!filePath.exists()) {
            logger.info("File not found: $filePath")
            return null
        }

        try {
            def image = ImageIO.read(filePath)
            if (image == null) {
                logger.info("ImageIO returned null for file: $filePath")
                return null
            }
            return [width: image.getWidth(), height: image.getHeight()]
        } catch (IOException e) {
            logger.info("Error reading the image file $filePath: ${e.message}")
            return null
        }
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
    static void runPythonCommand(String anacondaEnvPath, String pythonScriptPath, String x1, String y1, String x2, String y2) {
        try {
            def logger = LoggerFactory.getLogger(QuPathGUI.class)
            // Path to the Python executable in the Anaconda environment
            String pythonExecutable = "${anacondaEnvPath}/python.exe";

            // Combine coordinates into a single argument
            String coordinatesArg = "${x1},${y1},${x2},${y2}";
            logger.info("${x1},${y1},${x2},${y2}")
            // Construct the command
            String command = "\"${pythonExecutable}\" \"${pythonScriptPath}\" ${coordinatesArg}";
            logger.info(command)
            // Execute the command
            Process process = command.execute();
            process.waitFor();

            // Read the output
            process.inputStream.eachLine { line -> println line };
            process.errorStream.eachLine { line -> System.err.println line };
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}