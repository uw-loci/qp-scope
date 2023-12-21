package qupath.ext.qp_scope;

import javafx.scene.control.MenuItem
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.ext.qp_scope.functions.*
import org.slf4j.LoggerFactory

/**
 * Built from the QuPath extension template - an extension to control a microscope through a Python interface
 */
class QP_scope implements QuPathExtension {

	// Setting the variables here is enough for them to be available in the extension
	String name = "Microscopy in QuPath"
	String description = "Interact with a microscope from QuPath via Python interfaces like PycroManager or PyMMCore"
	Version QuPathVersion = Version.parse("v0.4.4")

//	@Override
//	void installExtension(QuPathGUI qupath) {
//		qupath.installActions(ActionTools.getAnnotatedActions(new BSCommands(qupath)))
//		addMenuItem(qupath)
//	}
	@Override
	void installExtension(QuPathGUI qupath) {
		addMenuItem(qupath)

	}
	/**
	 * Get the description of the extension.
	 *
	 * @return The description of the extension.
	 */
	@Override
	public String getDescription() {
		return "Control a microscope!";
	}

	/**
	 * Get the name of the extension.
	 *
	 * @return The name of the extension.
	 */
	@Override
	public String getName() {
		return "qp_scope";
	}

	private void addMenuItem(QuPathGUI qupath) {
		def logger = LoggerFactory.getLogger(QuPathGUI.class)
		//Check for dependencies and QuPath version
		logger.info("QuPath Version")
		logger.info(getQuPathVersion().toString())
		//TODO how to check if version is supported?

		def menu = qupath.getMenu("Extensions>${name}", true)
		def fileNameStitching = new MenuItem("Start qp_scope")
		// TODO tooltip
		fileNameStitching.setOnAction(e -> {
			//TODO check preferences for all necessary entries

			QP_scope_GUI.createGUI1()

		})
		menu.getItems() << fileNameStitching
	}
	
}
//@ActionMenu("Extensions")
//public class BSCommands {
//
//	@ActionMenu("BasicStitching")
//	@ActionDescription("Launch BasicStitching dialog.")
//	public final Action BSCommand;
//
//	/**
//	 * Constructor.
//	 *
//	 * @param qupath
//	 *            The QuPath GUI.
//	 */
//	private BSCommands(QuPathGUI qupath) {
//		BSMainCommand bsCommand = new BSMainCommand(qupath);
//		actionBSCommand = new Action(event -> bsCommand.run());
//	}
//
//}