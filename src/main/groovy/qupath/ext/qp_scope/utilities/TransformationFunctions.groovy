package qupath.ext.qp_scope.utilities

import org.slf4j.LoggerFactory
import qupath.ext.qp_scope.functions.QP_scope_GUI
import qupath.lib.gui.QuPathGUI
import qupath.lib.objects.PathObject

import java.awt.geom.AffineTransform
import java.awt.geom.NoninvertibleTransformException
import java.awt.geom.Point2D
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors

class TransformationFunctions {
    static final logger = LoggerFactory.getLogger(TransformationFunctions.class)

/**
 * Transforms coordinates from the QuPath coordinate system to the microscope stage coordinate system.
 * This function applies an AffineTransform to a given pair of QuPath coordinates, converting them
 * into the corresponding microscope stage coordinates.
 *
 * @param qpCoordinates The coordinates in the QuPath coordinate system, represented as a list of Double
 *                      where the first element is the x-coordinate and the second is the y-coordinate.
 * @param transformation The AffineTransform that defines the conversion from QuPath to microscope stage coordinates.
 * @return A list of Double representing the transformed coordinates in the microscope stage coordinate system,
 *         where the first element is the x-coordinate and the second is the y-coordinate.
 */
    static List<Double> QPtoMicroscopeCoordinates(List<Double> qpCoordinates, AffineTransform transformation) {
        logger.info("Transforming QP coordinates to microscope coordinates. Input QP Coordinates: ["
                + qpCoordinates.get(0) + ", " + qpCoordinates.get(1) + "]");

        Point2D.Double sourcePoint = new Point2D.Double(qpCoordinates.get(0), qpCoordinates.get(1));
        Point2D.Double destPoint = new Point2D.Double();

        transformation.transform(sourcePoint, destPoint);

        logger.info("Transformed Microscope Coordinates: [" + destPoint.x + ", " + destPoint.y + "]");

        return Arrays.asList(destPoint.x, destPoint.y);
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


//TODO adjust for situations where the macro image is flipped
    /**
     * Calculates an affine transformation based on scaling and translation.
     *
     * @param scalingTransform The AffineTransform object representing the scaling.
     * @param qpCoordinatesList A list of strings representing the coordinates in the qpCoordinates system.
     * @param stageCoordinatesList A list of strings representing the coordinates in the stageCoordinates system.
     * @return An AffineTransform object representing the combined scaling and translation.
     */
    /**
     * Calculates an affine transformation based on scaling and translation.
     *
     * @param scalingTransform The AffineTransform object representing the scaling.
     * @param qpCoordinatesList A list of strings representing the coordinates in the qpCoordinates system.
     * @param stageCoordinatesList A list of strings representing the coordinates in the stageCoordinates system.
     * @return An AffineTransform object representing the combined scaling and translation.
     */
    static AffineTransform initialTransformation(AffineTransform scalingTransform, List<String> qpCoordinatesList, List<String> stageCoordinatesList) {
        // Parse the coordinate strings to double
        logger.info("input scaling transform $scalingTransform")
        double[] qpPoint = qpCoordinatesList.collect { it.toDouble() } as double[]
        double[] mmPoint = stageCoordinatesList.collect { it.toDouble() } as double[]

        logger.info("Parsed qpPoint: ${qpPoint}")
        logger.info("Parsed mmPoint: ${mmPoint}")

        // Applying the scaling transform to qpPoint
        Point2D.Double scaledQpPoint = new Point2D.Double()
        scalingTransform.transform(new Point2D.Double(qpPoint[0], qpPoint[1]), scaledQpPoint)

        logger.info("Scaled qpPoint: ${scaledQpPoint}")

        // Calculate the translation vector, adjusted for scaling
        double tx = mmPoint[0] - scaledQpPoint.x
        double ty = mmPoint[1] - scaledQpPoint.y

        logger.info("Calculated translation: tx = ${tx}, ty = ${ty}")

        // Create the combined transform (scaling and translation)
        AffineTransform transform = new AffineTransform(scalingTransform)
        transform.translate(tx / scalingTransform.getScaleX(), ty / scalingTransform.getScaleY())

        logger.info("Final AffineTransform: ${transform}")

        return transform
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
        double scaleY = isSlideFlipped ? scale : -scale // Assume QuPath coordinate system is Y inverted from microscope stage
        //Inversion is usually going to be true because the Y axis in images is 0 at the top and Height at the bottom, while stages
        //tend to have a more normal coordinates system with increasing numbers going "up" the Y axis.

        transformation.scale(scale, scaleY)

        boolean gui4Success = QP_scope_GUI.stageToQuPathAlignmentGUI1()
        if (!gui4Success) {
            return null // End function early if the user cancels
        }
        return transformation
    }


}
