package qupath.ext.qp_scope.ui

import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import javafx.scene.control.*
import qupath.lib.scripting.QP

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType


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


//    static void showProgressBar(AtomicInteger progressCounter, int totalFiles) {
//        Platform.runLater(() -> {
//            long startTime = System.currentTimeMillis(); // Capture start time
//            logger.info("Creating progress bar with $totalFiles max")
//            progressBarStage = new Stage();
//            progressBarStage.initModality(Modality.NONE);
//            progressBarStage.setTitle("Microscope acquisition progress");
//
//            VBox vbox = new VBox(10); // Add spacing between elements
//            ProgressBar progressBar = new ProgressBar(0);
//            progressBar.setPrefWidth(300);
//            progressBar.setPrefHeight(20);
//            Label timeLabel = new Label("Estimating time..."); // Label to show time estimate
//            vbox.getChildren().addAll(progressBar, timeLabel);
//
//            Scene scene = new Scene(vbox);
//            progressBarStage.setScene(scene);
//            progressBarStage.setAlwaysOnTop(true);
//            progressBarStage.show();
//
//            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
//            executor.scheduleAtFixedRate(() -> {
//                double progress = progressCounter.get() / (double) totalFiles;
//                logger.info("Current Progress - updating bar to $progress via ${progressCounter.get()}")
//                long elapsedTime = System.currentTimeMillis() - startTime;
//                double timePerUnit = elapsedTime / (double) progressCounter.get();
//                int estimatedTotalTime = (int) (timePerUnit * totalFiles);
//                int remainingTime = (int) ((estimatedTotalTime - (int) elapsedTime) / 1000); // in seconds
//
//                Platform.runLater(() -> {
//                    progressBar.setProgress(progress);
//                    timeLabel.setText("Rough estimate of remaining time: " + remainingTime + " seconds");
//                    if (progressCounter.get() >= totalFiles) {
//                        progressBarStage.close();
//                        executor.shutdown();
//                    }
//                });
//            }, 0, 300, TimeUnit.MILLISECONDS);
//        });
//    }

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
                });



                // Stall check and process termination
                if (progress >= 1.0 || !pythonProcess.isAlive() || elapsedSinceLastUpdate > timeout) {
                    pythonProcess.destroy();
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
                }

                // If process is complete, close the progress bar
                if (!pythonProcess.isAlive()) {
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

    static void checkValidAnnotationsGUI(Closure callback) {
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
                        int annotationCount = QP.getAnnotationObjects().findAll { it.getPathClass()
                                .toString().equals("Tissue") }.size();
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

}
