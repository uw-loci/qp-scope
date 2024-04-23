package qupath.ext.qp_scope.tests


import org.slf4j.LoggerFactory
import qupath.ext.qp_scope.utilities.MinorFunctions
import qupath.ext.qp_scope.utilities.UtilityFunctions
import qupath.lib.scripting.QP
import qupath.lib.gui.scripting.QPEx
import qupath.lib.regions.ImagePlane
import qupath.lib.roi.ROIs
import qupath.lib.objects.PathObjects
import qupath.ext.qp_scope.utilities.TransformationFunctions
import java.awt.geom.AffineTransform


class CoordinateTransformationTest {
    static final logger = LoggerFactory.getLogger(CoordinateTransformationTest.class)

    static final String IMAGE_PATH = "C:/ImageAnalysis/QPExtension0.5.0/data/slides/blank.tif"
    static final String TEST_FOLDER = "C:/ImageAnalysis/QPExtension0.5.0/data/test"
    static final int IMAGE_WIDTH = 1000
    static final int IMAGE_HEIGHT = 1000
    static final double ORIGINAL_PIXEL_SIZE_MICRONS = 1.108
    static final double ACQUISITION_PIXEL_SIZE_MICRONS = 0.2201
    static final int CAMERA_WIDTH_PIXELS = 1392
    static final int CAMERA_HEIGHT_PIXELS = 1040

    static preferences = QPEx.getQuPath().getPreferencePane().getPropertySheet().getItems()

    static void main(String[] args) {
        // Step 1: Setup the image and annotation properties
        println("Starting transformation test...")
        def plane = ImagePlane.getDefaultPlane()
        def roi = ROIs.createRectangleROI(200, 200, 600, 600, plane)
        def tissue = PathObjects.createAnnotationObject(roi, QP.getPathClass("Tissue"))
        tissue.setName("Default box")
        //The tiling function expects an array
        tissue = [tissue]
        Double acquiredImageFrameWidth = ACQUISITION_PIXEL_SIZE_MICRONS*CAMERA_WIDTH_PIXELS
        Double acquiredImageFrameHeight = ACQUISITION_PIXEL_SIZE_MICRONS*CAMERA_HEIGHT_PIXELS

        // Step 2: Transform QuPath annotation to stage coordinates
        Double frameWidthQPpixels = (ACQUISITION_PIXEL_SIZE_MICRONS)*(CAMERA_WIDTH_PIXELS) / (ORIGINAL_PIXEL_SIZE_MICRONS)  //* (ACQUIRED_PIXEL_SIZE_MICRONS)
        Double frameHeightQPpixels = (ACQUISITION_PIXEL_SIZE_MICRONS)* (CAMERA_HEIGHT_PIXELS) / (ORIGINAL_PIXEL_SIZE_MICRONS) //* (ACQUIRED_PIXEL_SIZE_MICRONS)
        logger.info("frameWidthQPPixels = $frameWidthQPpixels")

        UtilityFunctions.performTilingAndSaveConfiguration(TEST_FOLDER,
                "test_1",
                frameWidthQPpixels,
                frameHeightQPpixels,
                0,
                null,
                true,
                tissue)

        def relativePixelSize = 1/ORIGINAL_PIXEL_SIZE_MICRONS
        ///////////////////////////////////////////////
        //AffineTransform scalingTransform = TransformationFunctions.setupAffineTransformationAndValidationGUI(relativePixelSize as Double, preferences as ObservableListWrapper)
        AffineTransform scalingTransform = new AffineTransform() // Start with the identity matrix


        boolean invertedXAxis = preferences.find{it.getName() == "Inverted X stage"}.getValue() as Boolean
        boolean invertedYAxis = preferences.find{it.getName() == "Inverted Y stage"}.getValue() as Boolean
        double scale =  relativePixelSize
        double scaleX = invertedXAxis ? -scale : scale
        double scaleY = invertedYAxis ? -scale : scale

        //Inversion is usually going to be true because the Y axis in images is 0 at the top and Height at the bottom, while stages
        //tend to have a more normal coordinates system with increasing numbers going "up" the Y axis.

        scalingTransform.scale(scaleX, scaleY)
//////////////////////////////////
        //Assume user has moved stage to the "correct" position, 5000, 5000
        //Assume the tile selected is the upper left one, at "400", "400"
        List<Double> coordinatesQP = [1133.7d, 4253.0d]
        coordinatesQP.each { item -> logger.info("Type of qpCoordinatesList item: ${item.getClass().getName()} - Value: $item") }


        List<String> currentStageCoordinates_um_String = ["18838", "13730"]
        logger.info("Obtained stage coordinates: $currentStageCoordinates_um_String")
        logger.info("QuPath coordinates for selected tile: $coordinatesQP")
        logger.info("affine transform before initial alignment: $scalingTransform")
        List<Double> currentStageCoordinates_um = MinorFunctions.convertListToDouble(currentStageCoordinates_um_String)

        //Create offsets beteween QuPath's idea of the center of a tile and the stage position
        double offsetX = -0.5 * CAMERA_WIDTH_PIXELS * (ACQUISITION_PIXEL_SIZE_MICRONS)
        double offsetY =-0.5 * CAMERA_HEIGHT_PIXELS * (ACQUISITION_PIXEL_SIZE_MICRONS)
        def offset = [offsetX, offsetY]
        AffineTransform transform = TransformationFunctions.addTranslationToScaledAffine(scalingTransform, coordinatesQP, currentStageCoordinates_um, offset)
        logger.info("affine transform after initial alignment: $transform")
        logger.info("offsets: $offset")
        def listOfQuPathTileCoordinates = [[1133.7d, 4253.0d], [1548.5, 4150], [1272, 4356.5]]
        def listOfExpectedStageCoordinates = [[18838, 13730], [19143, 13954],[18838, 13709]]

        def tileconfigFolders = TransformationFunctions.transformTileConfiguration(TEST_FOLDER, transform)
        for (folder in tileconfigFolders) {
            logger.info("modified TileConfiguration at $folder")
        }
        def transformedPoint = []
        for (point in listOfQuPathTileCoordinates) {
            transformedPoint.add(TransformationFunctions.QPtoMicroscopeCoordinates(point as List<Double>, transform))
        }
        logger.info("Converted $listOfQuPathTileCoordinates ")
        logger.info("to $transformedPoint")
        logger.info("Expected $listOfExpectedStageCoordinates")

        def transformedPoint2 = []
        for (point in listOfQuPathTileCoordinates) {
            transformedPoint2.add(TransformationFunctions.QPtoMicroscopeCoordinates(point as List<Double>, scalingTransform))
        }
        logger.info("Converted $listOfQuPathTileCoordinates ")
        logger.info("to $transformedPoint2")
        logger.info("Expected $listOfExpectedStageCoordinates")
        def firstPoint = (transformedPoint2[0] as List<Double>)[0]
        def secondPoint = (transformedPoint2[1] as List<Double>)[0]
        logger.info("first point $firstPoint")
        logger.info("second point $secondPoint")
        def difference = (firstPoint - secondPoint)
        logger.info("Difference in X is : ${difference}, expecting 305 ish")

        println("Transformation test completed.")
    }


}


