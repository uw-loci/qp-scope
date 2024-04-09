package qupath.ext.qp_scope.utilities

import com.sun.javafx.collections.ObservableListWrapper
import org.slf4j.LoggerFactory
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.commands.ProjectCommands
import qupath.fx.dialogs.Dialogs
import qupath.lib.gui.images.stores.ImageRegionStoreFactory
import qupath.lib.gui.scripting.QPEx
import qupath.lib.gui.tools.GuiTools
import qupath.lib.images.ImageData
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.images.servers.ImageServers
import qupath.lib.images.servers.TransformedServerBuilder
import qupath.lib.projects.Project
import qupath.lib.projects.ProjectIO
import qupath.lib.projects.ProjectImageEntry
import qupath.lib.projects.Projects
import qupath.lib.scripting.QP
import qupath.lib.gui.images.stores.DefaultImageRegionStore

import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

class QPProjectFunctions {
    static final logger = LoggerFactory.getLogger(QPProjectFunctions.class)

    /**
     * Creates a new QuPath project, adds the current image to it, and opens the project.
     *
     * @param projectsFolderPath The path where the project will be located.
     * @param sampleLabel The label for the sample.
     * @param preferences User preferences that include settings like scan type.
     * @return A map containing the
     * created project
     * temporary tile directory String
     * matchingImage ProjectImageEntry
     * imagingModeWithIndex string
     */
    static Map<String, Object> createAndOpenQuPathProject(QuPathGUI qupathGUI,
                                                          String projectsFolderPath,
                                                          String sampleLabel,
                                                          ObservableListWrapper preferences,
                                                          boolean isSlideFlippedX=false,
                                                          boolean isSlideFlippedY=false) {
        String firstImagingMode = preferences.find{it.getName() == "First Scan Type"}.getValue() as String
        Project currentQuPathProject = createProjectFolder(projectsFolderPath, sampleLabel)

        String imagingModeWithIndex = MinorFunctions.getUniqueFolderName(projectsFolderPath + File.separator + sampleLabel + File.separator + firstImagingMode)
        String tempTileDirectory = projectsFolderPath + File.separator + sampleLabel + File.separator + imagingModeWithIndex

        String macroImagePath = null
        ProjectImageEntry matchingImage = null

        if (QP.getCurrentImageData() != null) {
            logger.info("current image exists")
            String serverPath = QP.getCurrentImageData().getServerPath()
            logger.info(serverPath)
            macroImagePath = MinorFunctions.extractFilePath(serverPath)
            logger.info(macroImagePath)
            if (macroImagePath != null) {
                logger.info("Extracted file path: $macroImagePath")
                addImageToProject(new File(macroImagePath), currentQuPathProject, isSlideFlippedX, isSlideFlippedY)
                logger.info("image added to project")
                qupathGUI.setProject(currentQuPathProject)
                matchingImage = currentQuPathProject.getImageList().find { image ->
                    new File(image.getImageName()).name == new File(macroImagePath).name
                }
                logger.info("open image ")
                qupathGUI.openImageEntry(matchingImage)
                qupathGUI.refreshProject()
            } else {
                logger.info("File path could not be extracted.")
            }
        } else {
            logger.info("No current image data available.")
        }



        return [
                'matchingImage': matchingImage,
                'imagingModeWithIndex': imagingModeWithIndex,
                'currentQuPathProject': currentQuPathProject,
                'tempTileDirectory': tempTileDirectory
        ]
    }

    static Map<String, Object> getCurrentProjectInformation(String projectsFolderPath, String sampleLabel, ObservableListWrapper preferences, String imagingModality){

        Project currentQuPathProject = QP.getProject()
        String firstImagingMode = preferences.find{it.getName() == imagingModality}.getValue() as String
        String imagingModeWithIndex = MinorFunctions.getUniqueFolderName(projectsFolderPath + File.separator + sampleLabel + File.separator + firstImagingMode)
        String tempTileDirectory = projectsFolderPath + File.separator + sampleLabel + File.separator + imagingModeWithIndex
        ProjectImageEntry matchingImage = QP.getProjectEntry()

        return [
                'matchingImage': matchingImage,
                'imagingModeWithIndex': imagingModeWithIndex,
                'currentQuPathProject': currentQuPathProject,
                'tempTileDirectory': tempTileDirectory
        ]
    }

    static boolean addImageToProject(File stitchedImagePath,
                                     Project project,
                                     boolean isSlideFlippedX = false,
                                     boolean isSlideFlippedY = false) {

        def imagePath = stitchedImagePath.toURI().toString()

        // Add the image as entry to the project
        logger.info("Adding: " + imagePath)
        if (project == null) {
            logger.warn("Project is null, there must have been a problem creating the project")
            return false
        }
        def originalImageServer = ImageServers.buildServer(imagePath)
        AffineTransform transform = new AffineTransform()
        // Determine scale factors based on the flip booleans
        double scaleX = isSlideFlippedX ? -1 : 1;
        double scaleY = isSlideFlippedY ? -1 : 1;

        // Apply scaling with flip conditions
        transform.scale(scaleX, scaleY);
        // If flipping on X-axis, also adjust the translation to reposition the image correctly
        if (isSlideFlippedX) {
            transform.translate(-originalImageServer.getWidth(), 0);
        }

        // If flipping on Y-axis, adjust the translation to reposition the image correctly
        if (isSlideFlippedY) {
            transform.translate(0, -originalImageServer.getHeight());
        }

        def flippedImageServer = new TransformedServerBuilder(originalImageServer)
                .transform(transform)
                .build()
        def imageEntry = project.addImage(flippedImageServer.getBuilder())

        // Set a particular image type
        ImageData imageData = imageEntry.readImageData()
        logger.info(imageData.toString())
        // https://forum.image.sc/t/creating-project-from-command-line/45608/24

        def imageRegionStore = QPEx.getQuPath().getImageRegionStore()
        def imageType = GuiTools.estimateImageType(imageData.getServer(), imageRegionStore.getThumbnail(imageData.getServer(), 0, 0, true));
        imageData.setImageType(imageType)
        imageEntry.saveImageData(imageData)



        // Add an entry name (the filename)
        imageEntry.setImageName(stitchedImagePath.getName())
        project.syncChanges()
        return true

    }

    static Project createProjectFolder(String projectsFolderPath, String sampleLabel) {
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
                Dialogs.showErrorNotification("Warning!", "Multiple Project files found, may cause unexpected behavior!")
            }

            project = ProjectIO.loadProject(qpprojFiles[0], BufferedImage.class)
        }

        if (project == null) {
            Dialogs.showErrorNotification("Warning!", "Project is null!")
        }
        // Within projectsFolderPath, check for a folder with the name "SlideImages", if it does not exist, create it
        String slideImagesFolderPathStr = projectsFolderPath + File.separator + sampleLabel + File.separator + "SlideImages"
        File slideImagesFolder = new File(slideImagesFolderPathStr)

        if (!slideImagesFolder.exists()) {
            slideImagesFolder.mkdirs()
        }

        return project
    }

}
