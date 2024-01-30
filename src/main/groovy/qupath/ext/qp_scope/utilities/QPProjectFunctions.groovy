package qupath.ext.qp_scope.utilities

import org.slf4j.LoggerFactory
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.commands.ProjectCommands
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.gui.images.stores.ImageRegionStoreFactory
import qupath.lib.gui.tools.GuiTools
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.projects.Project
import qupath.lib.projects.ProjectIO
import qupath.lib.projects.ProjectImageEntry
import qupath.lib.projects.Projects
import qupath.lib.scripting.QP

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
     * scanTypeWithIndex string
     */
    static Map<String, Object> createAndOpenQuPathProject(QuPathGUI qupathGUI, String projectsFolderPath, String sampleLabel, Map<String,String> preferences) {
        Project currentQuPathProject = createProjectFolder(projectsFolderPath, sampleLabel, preferences.firstScanType)

        String scanTypeWithIndex = MinorFunctions.getUniqueFolderName(projectsFolderPath + File.separator + sampleLabel + File.separator + preferences.firstScanType)
        String tempTileDirectory = projectsFolderPath + File.separator + sampleLabel + File.separator + scanTypeWithIndex

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
                addImageToProject(new File(macroImagePath), currentQuPathProject)
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
                'scanTypeWithIndex': scanTypeWithIndex,
                'currentQuPathProject': currentQuPathProject,
                'tempTileDirectory': tempTileDirectory
        ]
    }

    static Map<String, Object> getCurrentProjectInformation(String projectsFolderPath, String sampleLabel, Map<String,String> preferences){

        Project currentQuPathProject = QP.getProject()

        String scanTypeWithIndex = MinorFunctions.getUniqueFolderName(projectsFolderPath + File.separator + sampleLabel + File.separator + preferences.firstScanType)
        String tempTileDirectory = projectsFolderPath + File.separator + sampleLabel + File.separator + scanTypeWithIndex
        ProjectImageEntry matchingImage = QP.getProjectEntry()

        return [
                'matchingImage': matchingImage,
                'scanTypeWithIndex': scanTypeWithIndex,
                'currentQuPathProject': currentQuPathProject,
                'tempTileDirectory': tempTileDirectory
        ]
    }

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
        // Write a thumbnail if we can
        var img = ProjectCommands.getThumbnailRGB(imageData.getServer())
        entry.setThumbnail(img)
        // Set a particular image type automatically (based on /qupath/lib/gui/QuPathGUI.java#L2847)
        // https://forum.image.sc/t/creating-project-from-command-line/45608/24
        def imageRegionStore = ImageRegionStoreFactory.createImageRegionStore(QuPathGUI.getTileCacheSizeBytes());
        def imageType = GuiTools.estimateImageType(imageData.getServer(), imageRegionStore.getThumbnail(imageData.getServer(), 0, 0, true));
        imageData.setImageType(imageType)
        //imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
        entry.saveImageData(imageData)



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

}
