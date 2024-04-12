package qupath.ext.qp_scope.ui

import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.Window
import org.slf4j.LoggerFactory
import javafx.scene.control.*
import qupath.ext.qp_scope.utilities.MinorFunctions
import qupath.ext.qp_scope.utilities.TransformationFunctions
import qupath.ext.qp_scope.utilities.UtilityFunctions
import qupath.lib.gui.QuPathGUI
import qupath.lib.objects.PathObject
import qupath.lib.scripting.QP

import java.awt.geom.AffineTransform
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType

import java.util.stream.Collectors


class UI_functions {
    static final logger = LoggerFactory.getLogger(UI_functions.class)
    static Stage progressBarStage;

    static void addToGrid(GridPane pane, Node label, Node control, int rowIndex) {
        pane.add(label, 0, rowIndex)
        pane.add(control, 1, rowIndex)
    }

    // Overloaded addToGrid method for a single Node
    // TODO fix hardcoding of 2 and 1
    static void addToGrid(GridPane pane, Node node, int rowIndex) {
        pane.add(node, 0, rowIndex, 2, 1) // The node spans 2 columns
    }
    // Helper method to check if input is numeric
    static boolean isValidInput(String input) {
        return input.matches("\\d*")
    }



/**
 * Displays a progress bar and updates it based on progress of a Python process.
 *
 * @param progressCounter A thread-safe counter tracking the number of processed files.
 * @param totalFiles The total number of files to process.
 * @param pythonProcess The Python process whose progress is being monitored.
 * @param timeout Timeout in milliseconds after which the process is considered stalled and is terminated.
 */
    static void showProgressBar(AtomicInteger progressCounter, int totalFiles, Process pythonProcess, int timeout) { // Added Process pythonProcess parameter
        Platform.runLater(() -> {
            //long startTime = System.currentTimeMillis(); // Capture start time
            AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
            AtomicLong lastUpdateTime = new AtomicLong(System.currentTimeMillis()); // Track the last update time
            AtomicInteger lastProgress = new AtomicInteger(0); // Track the progress during the last update


            logger.info("Creating progress bar with " + totalFiles + " max");
            progressBarStage = new Stage();
            progressBarStage.initModality(Modality.NONE);
            progressBarStage.setTitle("Microscope acquisition progress");

            VBox vbox = new VBox(10); // Add spacing between elements
            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(300);
            progressBar.setPrefHeight(20);
            Label timeLabel = new Label("Estimating time..."); // Label to show time estimate
            Label progressLabel = new Label("Processing files...");
            vbox.getChildren().addAll(progressBar, timeLabel,progressLabel);

            Scene scene = new Scene(vbox);
            progressBarStage.setScene(scene);
            progressBarStage.setAlwaysOnTop(true);
            progressBarStage.show();


            // Executor for updating the progress bar periodically
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(() -> {
                int currentProgress = progressCounter.get()
                long currentTime = System.currentTimeMillis();
                double progress = progressCounter.get() / (double) totalFiles;
                //Estimate time remaining
                long elapsedTime = currentTime - startTime.get();
                long elapsedSinceLastUpdate = currentTime - lastUpdateTime.get();
                double timePerUnit = progressCounter.get() > 0 ? elapsedTime / (double) progressCounter.get() : 0;
                int estimatedTotalTime = (int) (timePerUnit * totalFiles);
                int remainingTime = (int) ((estimatedTotalTime - elapsedTime) / 1000); // in seconds

                // Update progress bar and label on the JavaFX Application Thread
                Platform.runLater(() -> {
                    progressBar.setProgress(progress);
                    timeLabel.setText("Rough estimate of remaining time: " + remainingTime + " seconds");
                    progressLabel.setText(String.format("Processed %d out of %d files...", progressCounter.get(), totalFiles));
                    //logger.info("current files are ${progressCounter.get()}")
                });



                // Stall check and process termination
                if (!pythonProcess.isAlive() || elapsedSinceLastUpdate > timeout) {
                    logger.info("final progress: $progress")
                    logger.info( "Python process: ${pythonProcess.isAlive()}")
                    logger.info("Elapsed time: $elapsedSinceLastUpdate")

                    // Executor shutdown moved outside Platform.runLater
                    executor.shutdownNow();
                    Platform.runLater(() -> {
                        progressLabel.setText("Process stalled and was terminated.");
                        notifyUserOfError("Timeout reached when waiting for images from microscope.\n Acquisition halted.",
                                "Acquisition process.")
                        // Delayed closing of progress bar window
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000); // Wait for 2 seconds before closing the window
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            logger.info("Progress bar closing")
                            Platform.runLater(progressBarStage::close);
                        }).start();
                    });
                } else if (currentProgress > lastProgress.get()) {
                    lastUpdateTime.set(currentTime); // Update the last update time only if progress has changed
                    lastProgress.set(currentProgress); // Update the last known progress
                } else if (progress >= 1.0){
                    Platform.runLater(() -> {
                        logger.info("Progress bar closing")
                        progressBarStage.close();
                        executor.shutdownNow(); // Ensure the executor is stopped
                    });
                }

            }, 200, 200, TimeUnit.MILLISECONDS);
        });
    }

    static void notifyUserOfError(String errorMessage, String actionContext) {
        Platform.runLater(new Runnable() {
            @Override
            void run() {
                Alert alert = new Alert(AlertType.ERROR)
                alert.setTitle("Error")
                alert.setHeaderText("Error during $actionContext")
                alert.setContentText(errorMessage)
                alert.initModality(Modality.APPLICATION_MODAL) // Ensure dialog is modal and on top
                alert.showAndWait()
            }
        })
    }

    static void checkValidAnnotationsGUI(List<String> validAnnotationNames, Closure callback) {
        Platform.runLater(new Runnable() {
            void run() {
                Stage stage = new Stage()
                stage.initModality(Modality.NONE) // Non-blocking
                stage.title = "Validate annotation boundaries"
                stage.alwaysOnTop = true // Ensure the dialog stays on top

                VBox layout = new VBox(10)
                Label infoLabel = new Label("Checking annotations...")
                Button collectButton = new Button("Collect regions")
                Button doNotCollectButton = new Button("Do not collect ANY regions")

                collectButton.setOnAction({ e ->
                    logger.info( "Collect regions selected.")
                    stage.close()
                    callback.call(true)
                    // No explicit cast needed here
                })

                doNotCollectButton.setOnAction({ e ->
                    logger.info("Do not collect, cancelled out of dialog.")
                    stage.close()
                    callback.call(false)
                    // No explicit cast needed here
                })
                var executor = Executors.newSingleThreadScheduledExecutor();
                executor.scheduleAtFixedRate(() -> {
                    Platform.runLater(() -> {
                        // Assuming QP.getAnnotationObjects() is the correct method to retrieve the current annotations
                        // This might need to be adjusted based on your actual API for accessing annotations
                        int annotationCount = QP.getAnnotationObjects().findAll { validAnnotationNames.contains(it?.getPathClass()?.toString())}.size();
                        infoLabel.setText("Total Annotation count in image to be processed: " + annotationCount +
                                "\nADD, MODIFY or DELETE annotations to select regions to be scanned." +
                                "\nEnsure that any newly created annotations are classified as 'Tissue'");
                        collectButton.setText("Collect " + annotationCount + " regions");
                    });
                }, 0, 500, TimeUnit.MILLISECONDS);


                layout.children.addAll(infoLabel, collectButton, doNotCollectButton)
                Scene scene = new Scene(layout, 400, 200)
                stage.scene = scene
                stage.show() // Non-blocking show
            }
        })
    }


    /**
     * Displays a dialog to the user for tile selection to match the live view in the microscope with QuPath's coordinate system.
     * This function ensures that exactly one tile (detection) is selected by the user. If the selection does not meet
     * the criteria (exactly one detection), the user is prompted until a valid selection is made or the operation is cancelled.
     *
     * The dialog is set to always be on top to ensure visibility to the user. If the user cancels or closes the dialog
     * without making a valid selection, the function returns false to indicate the operation was not completed.
     *
     * @return true if a valid tile is selected and the user confirms the selection; false if the user cancels the operation.
     */
    static boolean stageToQuPathAlignmentGUI1() {
        boolean validTile = false;
        Optional<ButtonType> result;

        while (!validTile) {
            // Create and configure the dialog inside the loop
            Dialog<ButtonType> dlg = new Dialog<>();
            dlg.initModality(Modality.NONE);
            dlg.setTitle("Identify Location");
            dlg.setHeaderText("Select one tile (a detection) and match the Live View in uManager to the location of that tile, as closely as possible.\n This will be used for matching QuPath's coordinate system to the microscope stage coordinate system, so be as careful as you can!");
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dlg.setOnShown(event -> {
                Window window = dlg.getDialogPane().getScene().getWindow();
                if (window instanceof Stage) {
                    ((Stage) window).setAlwaysOnTop(true);
                }
            });

            // Show the dialog and wait for the user response
            result = dlg.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                List selectedObjects = QP.getSelectedObjects().stream()
                        .filter(object -> object.isDetection() && object.getROI() instanceof qupath.lib.roi.RectangleROI)
                        .collect(Collectors.toList());

                if (selectedObjects.size() != 1) {
                    MinorFunctions.showAlertDialog("There needs to be exactly one tile selected.");
                } else {
                    validTile = true;
                }
            } else {
                // User cancelled or closed the dialog
                return false;
            }
        }
        return true;
    }


/**
 * Displays a dialog to confirm the accuracy of the current stage position in comparison with the live view.
 * This dialog is part of the process to calibrate the "Live view" stage position with the position of a single field of view in the preview image.
 * The user is presented with two options: to confirm the current position is accurate or to cancel the acquisition.
 *
 * @return {@code true} if the user confirms the current position is accurate, {@code false} if the user cancels the acquisition.
 */
    static boolean stageToQuPathAlignmentGUI2() {
        // Create a custom dialog with application modal modality.
        Dialog<Boolean> dlg = new Dialog<>();
        dlg.initModality(Modality.APPLICATION_MODAL); // Prevents interaction with other windows until this dialog is closed.
        dlg.setTitle("Position Confirmation");
        // Header text explaining the purpose of the dialog and providing instructions for comparison.
        dlg.setHeaderText("Is the current position accurate? \nCompare with the uManager live view!\n" +
                "The first time this dialog shows up, it should select the center of the top row!\n" +
                "The second time, it should select the center of the left-most column!");
        dlg.setOnShown(event -> {
            Window window = dlg.getDialogPane().getScene().getWindow();
            if (window instanceof Stage) {
                ((Stage) window).setAlwaysOnTop(true);
            }
        });
        // Define button types for user actions.
        ButtonType btnCurrentPositionAccurate = new ButtonType("Current Position is Accurate", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelAcquisition = new ButtonType("Cancel acquisition", ButtonBar.ButtonData.CANCEL_CLOSE);

        // Add buttons to the dialog pane.
        dlg.getDialogPane().getButtonTypes().addAll(btnCurrentPositionAccurate, btnCancelAcquisition);

        // Process the user's button click to determine the return value.
        dlg.setResultConverter(dialogButton -> {
            if (dialogButton == btnCurrentPositionAccurate) {
                // If the user confirms the current position is accurate, return true.
                return true;
            } else if (dialogButton == btnCancelAcquisition) {
                // If the user cancels the acquisition, return false.
                return false;
            }
            return null; // Return null if another close mechanism is triggered.
        });

        // Show the dialog and wait for the user to make a selection, then return the result.
        Optional<Boolean> result = dlg.showAndWait();

        // Return the user's decision or false if no explicit decision was made.
        return result.orElse(false);
    }


    /**
     * Handles the process of selecting a tile, transforming its coordinates, moving the stage,
     * validating the new stage position, and updating the transformation.
     *
     * @param tileXY The tile coordinates and object.
     * @param qupathGUI The QuPath GUI instance.
     * @param virtualEnvPath
     The virtual environment path for Python commands.

     @param pythonScriptPath The Python script path.

     @param transformation The current AffineTransform.

     @return A boolean indicating if the position was validated successfully and the updated transformation.
     */

    static Map<String, Object> handleStageAlignment(PathObject tileXY, QuPathGUI qupathGUI,
                                                            String virtualEnvPath, String pythonScriptPath,
                                                            AffineTransform transformation, List<Double> offset) {
        QP.selectObjects(tileXY)
        // Transform the QuPath coordinates into stage coordinates
        def QPPixelCoordinates = [tileXY.getROI().getCentroidX(), tileXY.getROI().getCentroidY()]
        List expectedStageXYPositionMicrons = TransformationFunctions.QPtoMicroscopeCoordinates(QPPixelCoordinates, transformation)
        logger.info("QuPath pixel coordinates: $QPPixelCoordinates")
        logger.info("Transformed into stage coordinates: $expectedStageXYPositionMicrons")
        // Move the stage to the new coordinates
        UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, expectedStageXYPositionMicrons as List<String>, "moveStageToCoordinates.py")
        qupathGUI.getViewer().setCenterPixelLocation(tileXY.getROI().getCentroidX(), tileXY.getROI().getCentroidY())

        // Validate the position that was moved to or update with an adjusted position
        def updatePosition = stageToQuPathAlignmentGUI2()
        if (updatePosition.equals("Use adjusted position")) {
            // Get access to current stage coordinates and update transformation
            List currentStageCoordinates_um = UtilityFunctions.runPythonCommand(virtualEnvPath, pythonScriptPath, null, "getStageCoordinates.py")

            transformation = TransformationFunctions.addTranslationToScaledAffine(transformation, QPPixelCoordinates as List<Double>, currentStageCoordinates_um as List<Double>, offset)
        }

        // Prepare the results to be returned
        Map<String, Object> results = [
                'updatePosition': updatePosition,
                'transformation': transformation
        ]

        return results
    }
}
