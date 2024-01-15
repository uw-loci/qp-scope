//Exports TileConfiguration.txt file for a given area, annotation or bounding box
//CURRENTLY NAME-MODIFIED FOR PDAC PROJECT - CHANGE OUTPUT FILE NAMES IN both PLACES FOR ANY OTHER PROJECT
// updated 20220126 to remove 1st line apparent 1 tile indent when tiling
//Modified for use in new CAMM setup Jan 2024. Requires more refactoring to remove hardcoded values for target resolutions and overlap percent

//For the bounding box input, createTiles = false and boundingBox = [x1,y1,x2,y2] need to be set
//x1,y1 are assumed to be the TOP LEFT of the box
boundingBoxStageCoordinates_um = false 
createTiles = true

double pixelSizeSource = 7.2//1.105
double pixelSizeTarget = 1.105
double frameWidth = 1392 / pixelSizeSource * pixelSizeTarget
double frameHeight = 1040 / pixelSizeSource * pixelSizeTarget
double overlapPercent = 10
baseDirectory = "to be replaced"
imagingModality = "4x-bf"

//Potentially store tiles as they are created
newTiles = []


//Ensure the folder to store the csv exists
tilePath = buildFilePath(baseDirectory, imagingModality)
mkdirs(tilePath)


//If not bounding box is passed, assume we need to find annotations and tile those
if (!boundingBoxStageCoordinates_um){
    //imageName = GeneralTools.getNameWithoutExtension(getQuPath().getProject().getEntry(imageData).getImageName())
    imageData = getQuPath().getImageData()
    hierarchy = imageData.getHierarchy()
    clearDetections()
    //Check all annotations. Use .findAll{expression} to select a subset
    annotations = hierarchy.getAnnotationObjects()

    /***********************************************/
    //Name each annotation in the image by its XY centroids
    annotations.each {

        it.setName((int) it.getROI().getCentroidX() + "_" + (int) it.getROI().getCentroidY())

    }
    getAnnotationObjects().each { it.setLocked(true) }
    annotations.eachWithIndex { a, i ->
        annotationROI = a.getROI()
        bBoxX = annotationROI.getBoundsX()
        bBoxY = annotationROI.getBoundsY()
        bBoxH = annotationROI.getBoundsHeight()
        bBoxW = annotationROI.getBoundsWidth()
        annotationName = a.getName()
        //Ensure the folder to store the csv exists
        tilePath = buildFilePath(baseDirectory, imagingModality, annotationName)
        mkdirs(tilePath)
        newTiles = createTileConfiguration(bBoxX, bBoxY, bBoxW, bBoxH, frameWidth, frameHeight, overlapPercent, tilePath, annotationROI)
    }

}else{

    // Convert string coordinates to doubles and calculate width and height
    bBoxX = boundingBoxStageCoordinates_um[0]
    bBoxY = boundingBoxStageCoordinates_um[1]
    double x2 = boundingBoxStageCoordinates_um[2]
    double y2 = boundingBoxStageCoordinates_um[3]
    bBoxW = x2 - bBoxX
    bBoxH = y2 - bBoxY
    //print("creating bounding box") 
    annotationROI = new RectangleROI(bBoxX, bBoxY, bBoxW, bBoxH, ImagePlane.getDefaultPlane())

    //Ensure the folder to store the csv exists
    tilePath = buildFilePath(baseDirectory, imagingModality, "bounds")
    mkdirs(tilePath)
    createTileConfiguration(bBoxX, bBoxY, bBoxW, bBoxH, frameWidth, frameHeight, overlapPercent, tilePath, annotationROI)
}
/***********************************************/
//The bounding box is more complicated of a change than I thought, since we can't check for intersections, don't have an annotation name, etc.
if (createTiles) {
    getCurrentHierarchy().addObjects(newTiles)
    fireHierarchyUpdate()
}


// Function to perform tiling and save TileConfiguration.txt
def createTileConfiguration(double bBoxX, double bBoxY, double bBoxW, double bBoxH, double frameWidth, double frameHeight, double overlapPercent, String tilePath, annotationROI= null) {
    //print "calling tileconfiguration function"
    int predictedTileCount = 0
    int actualTileCount = 0
    List xy = []
    int yline = 0
    List newTiles = []
    double y = bBoxY
    double x = bBoxX
    double xStep = frameWidth - overlapPercent / 100 * frameWidth
    double yStep = frameHeight - overlapPercent / 100 * frameHeight

    while (y < bBoxY + bBoxH) {
        //In order to serpentine the resutls, there need to be two bounds for X now
        while ((x <= bBoxX + bBoxW) && (x >= bBoxX - bBoxW * overlapPercent / 100)) {

            //An ROI for the tile
            def tileROI = new RectangleROI(x, y, frameWidth, frameHeight, ImagePlane.getDefaultPlane())
            //Check if the tile intersects the annotation
            if (annotationROI.getGeometry().intersects(tileROI.getGeometry())) {

                tileDetection = PathObjects.createDetectionObject(tileROI, getPathClass(imagingModality))
                tileDetection.setName(predictedTileCount.toString())
                tileDetection.getMeasurementList().putMeasurement("TileNumber", actualTileCount)
                newTiles << tileDetection
                //print(newTiles)
                xy << [x, y]
                actualTileCount++
                //print(actualTileCount)
            } 
            if (yline % 2 == 0) {
                x = x + frameWidth - overlapPercent / 100 * frameWidth
            } else {
                x = x - (frameWidth - overlapPercent / 100 * frameWidth)
            }
            predictedTileCount++
        }
        y = y + frameHeight - overlapPercent / 100 * frameHeight
        if (yline % 2 == 0) {
            x = x - (frameWidth - overlapPercent / 100 * frameWidth)
        } else {
            x = x + frameWidth - overlapPercent / 100 * frameWidth
        }

        yline++
    }

    // Write TileConfiguration.txt
    String header = "dim = 2\n"
    new File(buildFilePath(tilePath, "TileConfiguration.txt")).withWriter { fw ->
        fw.writeLine(header)
        xy.eachWithIndex { coords, index ->
            String line = "${index}.tif; ; (${coords[0]}, ${coords[1]})"
            fw.writeLine(line)
        }
    }
    return newTiles
}



import qupath.lib.regions.ImagePlane
import qupath.lib.roi.RectangleROI

import static qupath.lib.gui.scripting.QPEx.getQuPath
import static qupath.lib.scripting.QP.*
