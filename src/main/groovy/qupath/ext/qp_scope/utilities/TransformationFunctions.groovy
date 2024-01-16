package qupath.ext.qp_scope.utilities

import org.slf4j.LoggerFactory
import qupath.ext.qp_scope.functions.QP_scope_GUI
import qupath.lib.gui.QuPathGUI
import qupath.lib.objects.PathObject

import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.util.regex.Matcher
import java.util.regex.Pattern

class TransformationFunctions {
    static final logger = LoggerFactory.getLogger(TransformationFunctions.class)

    //Convert the QuPath pixel based coordinates for a location into the MicroManager micron based stage coordinates

    static List<Double> QPtoMicroscopeCoordinates(List<Double> qpCoordinates, AffineTransform transformation) {
        Point2D.Double sourcePoint = new Point2D.Double(qpCoordinates[0], qpCoordinates[1])
        Point2D.Double destPoint = new Point2D.Double()

        transformation.transform(sourcePoint, destPoint)

        return [destPoint.x, destPoint.y]
    }

/**
 * Transforms the coordinates in TileConfiguration.txt files located in all child directories
 * of a specified parent directory, using an AffineTransform. It reads each file, applies the
 * transformation to each tile's coordinates, and writes the transformed coordinates back to a
 * new file in each directory.
 *
 * @param parentDirPath The path to the parent directory containing child directories with TileConfiguration.txt files.
 * @param transformation The AffineTransform to be applied to each tile's coordinates.
 * @return A list of folder names that contain TileConfiguration.txt files which were modified.
 */
    static List<String> transformTileConfiguration(String parentDirPath, AffineTransform transformation) {
        logger.info("entering transform Tileconfiguration modification function")
        logger.info(parentDirPath)
        logger.info(transformation.toString())
        System.out.println("AffineTransform: " + transformation)

        File parentDir = new File(parentDirPath)
        List<String> modifiedFolders = []

        // Check if the path is a valid directory
        if (!parentDir.isDirectory()) {
            System.err.println("Provided path is not a directory: $parentDirPath")
            return modifiedFolders
        }

        // Iterate over all child folders
        File[] subdirectories = parentDir.listFiles(new FileFilter() {
            @Override
            boolean accept(File file) {
                return file.isDirectory()
            }
        })

        if (subdirectories) {
            subdirectories.each { File subdir ->
                File tileConfigFile = new File(subdir, "TileConfiguration.txt")
                if (tileConfigFile.exists()) {
                    // Process the TileConfiguration.txt file
                    processTileConfigurationFile(tileConfigFile, transformation)
                    modifiedFolders.add(subdir.name)
                }
            }
        }

        return modifiedFolders
    }

    private static void processTileConfigurationFile(File tileConfigFile, AffineTransform transformation) {
        List<String> transformedLines = []
        Pattern pattern = Pattern.compile("\\d+\\.tif; ; \\((.*),\\s*(.*)\\)")

        tileConfigFile.eachLine { line ->
            Matcher m = pattern.matcher(line)
            if (m.find()) {  // Use 'find()' to search for a match in the line
                double x1 = Double.parseDouble(m.group(1))
                double y1 = Double.parseDouble(m.group(2))
                List<Double> qpCoordinates = [x1, y1]
                List<Double> transformedCoords = QPtoMicroscopeCoordinates(qpCoordinates, transformation)
                transformedLines.add(line.replaceFirst("\\(.*\\)", "(${transformedCoords[0]}, ${transformedCoords[1]})"))
            } else {
                transformedLines.add(line) // Add line as is if no coordinate match
            }
        }


        // Write the transformed lines to a new file
        File newTileConfigFile = new File(tileConfigFile.getParent(), "TileConfiguration_transformed.txt")
        newTileConfigFile.withWriter { writer ->
            transformedLines.each { writer.println(it) }
        }
    }

/**
 * Updates an AffineTransform based on the difference between coordinates in QPath and microscope stage.
 * It applies the existing transformation to the QPath coordinates and then adjusts the transformation
 * to align these with the given microscope stage coordinates.
 *
 * @param transformation The current AffineTransform object.
 * @param coordinatesQP  List of QPath coordinates (as Strings) to be transformed.
 * @param coordinatesMM  List of microscope stage coordinates (as Strings) for alignment.
 * @return An updated AffineTransform object that reflects the necessary shift to align QPath coordinates
 *         with microscope stage coordinates after scaling.
 */
//TODO adjust for situations where the macro image is flipped
    static AffineTransform updateTransformation(AffineTransform transformation, List<String> coordinatesQP, List<String> coordinatesMM) {
        // Convert coordinatesQP and coordinatesMM elements from String to Double
        double xQP = coordinatesQP[0].toDouble()
        double yQP = coordinatesQP[1].toDouble()
        double xMM = coordinatesMM[0].toDouble()
        double yMM = coordinatesMM[1].toDouble()

        // Apply the existing transformation to the QP coordinates
        Point2D.Double transformedPoint = new Point2D.Double()
        transformation.transform(new Point2D.Double(xQP, yQP), transformedPoint)

        // Calculate the additional translation needed
        double additionalXShift = xMM - transformedPoint.x
        double additionalYShift = yMM - transformedPoint.y

        logger.info("Additional xShift: $additionalXShift")
        logger.info("Additional yShift: $additionalYShift")

        // Create a new AffineTransform that includes this additional translation
        AffineTransform updatedTransformation = new AffineTransform(transformation)
        updatedTransformation.translate(additionalXShift, additionalYShift)

        return updatedTransformation
    }


    static PathObject getTopCenterTile(Collection<PathObject> detections) {
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

        return topCenterTile
    }

    static PathObject getLeftCenterTile(Collection<PathObject> detections) {
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

        return leftCenterTile
    }

/**
 * Sets up an AffineTransform for the QuPath project based on pixel size and slide orientation,
 * and performs an initial stage alignment validation.
 *
 * @param pixelSize The size of the pixels in the image.
 * @param isSlideFlipped A boolean indicating if the slide is flipped.
 * @param preferences A map containing user preferences and settings.
 * @param qupathGUI The QuPath GUI instance used for executing GUI-related operations.
 * @return An AffineTransform object set up based on the provided parameters, or null if the user cancels the operation.
 */
    static AffineTransform setupAffineTransformationAndValidationGUI(double pixelSize, boolean isSlideFlipped, Map preferences, QuPathGUI qupathGUI) {
        AffineTransform transformation = new AffineTransform() // Start with the identity matrix
        double scale =  (preferences.pixelSizeFirstScanType as Double) / pixelSize
        double scaleY = isSlideFlipped ? -scale : scale // Invert the Y axis if flip is true

        transformation.scale(scale, scaleY)

        boolean gui4Success = QP_scope_GUI.stageToQuPathAlignmentGUI1()
        if (!gui4Success) {
            return null // End function early if the user cancels
        }
        return transformation
    }


}
