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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


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


    static void showProgressBar(AtomicInteger progressCounter, int totalFiles) {
        Platform.runLater(() -> {
            long startTime = System.currentTimeMillis(); // Capture start time
            //logger.info("Creating progress bar with $totalFiles max")
            progressBarStage = new Stage();
            progressBarStage.initModality(Modality.NONE);
            progressBarStage.setTitle("Microscope acquisition progress");

            VBox vbox = new VBox(10); // Add spacing between elements
            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(300);
            progressBar.setPrefHeight(20);
            Label timeLabel = new Label("Estimating time..."); // Label to show time estimate
            vbox.getChildren().addAll(progressBar, timeLabel);

            Scene scene = new Scene(vbox);
            progressBarStage.setScene(scene);
            progressBarStage.setAlwaysOnTop(true);
            progressBarStage.show();

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(() -> {
                double progress = progressCounter.get() / (double) totalFiles;
                //logger.info("Current Progress - updating bar to $progress via ${progressCounter.get()}")
                long elapsedTime = System.currentTimeMillis() - startTime;
                double timePerUnit = elapsedTime / (double) progressCounter.get();
                int estimatedTotalTime = (int) (timePerUnit * totalFiles);
                int remainingTime = (int) ((estimatedTotalTime - (int) elapsedTime) / 1000); // in seconds

                Platform.runLater(() -> {
                    progressBar.setProgress(progress);
                    timeLabel.setText("Rough estimate of remaining time: " + remainingTime + " seconds");
                    if (progressCounter.get() >= totalFiles) {
                        progressBarStage.close();
                        executor.shutdown();
                    }
                });
            }, 0, 300, TimeUnit.MILLISECONDS);
        });
    }


}
