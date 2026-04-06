package org.engin;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.*;
import java.time.LocalTime;
import java.util.Random;
import java.util.logging.*;

public class MouseJigglerApp extends Application {

    private static final String BETWEEN_HOURS = "Between Hours";
    private static final String FOR_DURATION = "For Duration";
    private volatile boolean running = false;
    private Robot robot;
    private final Logger logger = Logger.getLogger(MouseJigglerApp.class.getName());
    private int idleTimeMinutes = 3;
    private int moveIntervalSeconds = 5;
    private final Random random = new Random();

    private Label statusLabel;
    private TextArea logArea;
    private ComboBox<String> modeComboBox;
    private Spinner<Integer> durationSpinner;
    private Spinner<Integer> startHourSpinner;
    private Spinner<Integer> startMinSpinner;
    private Spinner<Integer> endHourSpinner;
    private Spinner<Integer> endMinSpinner;
    private Label durationLabel;
    private Label startTimeLabel;
    private Label endTimeLabel;

    static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Mouse Jiggler");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        // Idle Time
        grid.add(new Label("Idle Time (min):"), 0, 0);
        Spinner<Integer> idleTimeSpinner = new Spinner<>(1, 60, idleTimeMinutes);
        grid.add(idleTimeSpinner, 1, 0);

        // Move Interval
        grid.add(new Label("Move Interval (sec):"), 0, 1);
        Spinner<Integer> moveIntervalSpinner = new Spinner<>(1, 60, moveIntervalSeconds);
        grid.add(moveIntervalSpinner, 1, 1);

        // Mode
        grid.add(new Label("Mode:"), 0, 2);
        modeComboBox = new ComboBox<>(FXCollections.observableArrayList("Infinite", FOR_DURATION, BETWEEN_HOURS));
        modeComboBox.setValue("Infinite");
        grid.add(modeComboBox, 1, 2);

        // Duration
        durationLabel = new Label("Duration (hours):");
        durationSpinner = new Spinner<>(1, 24, 1);
        grid.add(durationLabel, 0, 3);
        grid.add(durationSpinner, 1, 3);

        // Start Time
        startTimeLabel = new Label("Start Time (HH:mm):");
        startHourSpinner = new Spinner<>(0, 23, 9);
        startMinSpinner = new Spinner<>(0, 59, 0);
        VBox startTimeBox = new VBox(5, startHourSpinner, startMinSpinner);
        grid.add(startTimeLabel, 0, 4);
        grid.add(startTimeBox, 1, 4);

        // End Time
        endTimeLabel = new Label("End Time (HH:mm):");
        endHourSpinner = new Spinner<>(0, 23, 18);
        endMinSpinner = new Spinner<>(0, 59, 0);
        VBox endTimeBox = new VBox(5, endHourSpinner, endMinSpinner);
        grid.add(endTimeLabel, 0, 5);
        grid.add(endTimeBox, 1, 5);

        Button toggleButton = new Button("Start");
        toggleButton.setMaxWidth(Double.MAX_VALUE);
        grid.add(toggleButton, 0, 6, 2, 1);

        statusLabel = new Label("Status: Stopped");
        grid.add(statusLabel, 0, 7, 2, 1);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(6);
        grid.add(logArea, 0, 8, 2, 1);

        setupLogger();

        modeComboBox.setOnAction(e -> updateModeVisibility());
        updateModeVisibility();

        toggleButton.setOnAction(e -> toggleJiggler(toggleButton, idleTimeSpinner, moveIntervalSpinner));

        Scene scene = new Scene(grid, 400, 500);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void updateModeVisibility() {
        String selectedMode = modeComboBox.getValue();
        boolean isDurationMode = FOR_DURATION.equals(selectedMode);
        boolean isBetweenHoursMode = BETWEEN_HOURS.equals(selectedMode);

        durationLabel.setVisible(isDurationMode);
        durationSpinner.setVisible(isDurationMode);
        startTimeLabel.setVisible(isBetweenHoursMode);
        startHourSpinner.setVisible(isBetweenHoursMode);
        startMinSpinner.setVisible(isBetweenHoursMode);
        endTimeLabel.setVisible(isBetweenHoursMode);
        endHourSpinner.setVisible(isBetweenHoursMode);
        endMinSpinner.setVisible(isBetweenHoursMode);
    }

    private void setupLogger() {
        logger.setUseParentHandlers(false);
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                Platform.runLater(() -> logArea.appendText(logRecord.getMessage() + "\n"));
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };
        handler.setFormatter(new SimpleFormatter());
        logger.addHandler(handler);
    }

    private void toggleJiggler(Button button, Spinner<Integer> idleTimeSpinner, Spinner<Integer> moveIntervalSpinner) {
        if (running) {
            stopJiggler(button);
        } else {
            if (BETWEEN_HOURS.equals(modeComboBox.getValue()) && !validateTimeInputs()) {
                return;
            }
            idleTimeMinutes = idleTimeSpinner.getValue();
            moveIntervalSeconds = moveIntervalSpinner.getValue();
            logger.info(String.format("Idle time set to %d minutes.", idleTimeMinutes));
            logger.info(String.format("Move interval set to %d seconds.", moveIntervalSeconds));
            startJiggler(button);
        }
    }

    private boolean validateTimeInputs() {
        int startH = startHourSpinner.getValue();
        int startM = startMinSpinner.getValue();
        int endH = endHourSpinner.getValue();
        int endM = endMinSpinner.getValue();

        if (startH > endH || (startH == endH && startM >= endM)) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Start time must be before end time.");
            alert.showAndWait();
            return false;
        }
        return true;
    }

    private void startJiggler(Button button) {
        running = true;
        button.setText("Stop");
        statusLabel.setText("Status: Running");
        logArea.clear();
        logger.info("Mouse Jiggler started.");

        Thread.ofVirtual().start(() -> {
            try {
                robot = new Robot();
                long startTimeMillis = System.currentTimeMillis();
                long durationMillis = durationSpinner.getValue() * 3600 * 1000L;

                while (running) {
                    String mode = modeComboBox.getValue();
                    if (FOR_DURATION.equals(mode) && System.currentTimeMillis() - startTimeMillis >= durationMillis) {
                        logger.info("Duration reached.");
                        break;
                    } else if (BETWEEN_HOURS.equals(mode)) {
                        LocalTime now = LocalTime.now();
                        LocalTime start = LocalTime.of(startHourSpinner.getValue(), startMinSpinner.getValue());
                        LocalTime end = LocalTime.of(endHourSpinner.getValue(), endMinSpinner.getValue());
                        if (now.isBefore(start) || now.isAfter(end)) {
                            logger.info("Outside of scheduled hours.");
                            break;
                        }
                    }

                    Point currentPos = MouseInfo.getPointerInfo().getLocation();
                    Thread.sleep(moveIntervalSeconds * 1000L);
                    Point newPos = MouseInfo.getPointerInfo().getLocation();

                    if (!currentPos.equals(newPos)) {
                        Platform.runLater(() -> statusLabel.setText("Status: Paused"));
                        logger.info("Mouse movement detected. Pausing...");
                        Thread.sleep(idleTimeMinutes * 60 * 1000L);
                        Platform.runLater(() -> statusLabel.setText("Status: Running"));
                        continue;
                    }

                    int dx = random.nextInt(11) - 5;
                    int dy = random.nextInt(11) - 5;
                    robot.mouseMove(currentPos.x + dx, currentPos.y + dy);
                    logger.info("Mouse moved at " + LocalTime.now().withNano(0));
                }
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Jiggler thread interrupted", e);
            } catch (AWTException e) {
                logger.log(Level.SEVERE, "AWT Error in jiggler", e);
            } finally {
                Platform.runLater(() -> stopJiggler(button));
            }
        });
    }

    private void stopJiggler(Button button) {
        running = false;
        button.setText("Start");
        statusLabel.setText("Status: Stopped");
        logger.info("Mouse Jiggler stopped.");
    }
}