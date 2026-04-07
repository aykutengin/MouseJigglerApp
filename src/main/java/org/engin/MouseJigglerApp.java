package org.engin;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalTime;

public class MouseJigglerApp extends Application {

    private final JigglerEngine jigglerEngine = new JigglerEngine();

    private Stage primaryStage;
    private Label statusLabel;
    private TextArea logArea;
    private ComboBox<JigglerMode> modeComboBox;
    private Spinner<Integer> durationSpinner;
    private Spinner<Integer> startHourSpinner;
    private Spinner<Integer> startMinSpinner;
    private Spinner<Integer> endHourSpinner;
    private Spinner<Integer> endMinSpinner;
    private Label durationLabel;
    private Label startTimeLabel;
    private Label endTimeLabel;
    private VBox startTimeBox;
    private VBox endTimeBox;
    private Button toggleButton;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Mouse Jiggler");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);
        grid.setPrefWidth(400);

        // Idle Time
        grid.add(new Label("Idle Time (min):"), 0, 0);
        Spinner<Integer> idleTimeSpinner = new Spinner<>(1, 60, 3);
        grid.add(idleTimeSpinner, 1, 0);

        // Move Interval
        grid.add(new Label("Move Interval (sec):"), 0, 1);
        Spinner<Integer> moveIntervalSpinner = new Spinner<>(1, 60, 5);
        grid.add(moveIntervalSpinner, 1, 1);

        // Mode
        grid.add(new Label("Mode:"), 0, 2);
        modeComboBox = new ComboBox<>(FXCollections.observableArrayList(JigglerMode.values()));
        modeComboBox.setValue(JigglerMode.INFINITE);
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
        startTimeBox = new VBox(5, startHourSpinner, startMinSpinner);
        grid.add(startTimeLabel, 0, 4);
        grid.add(startTimeBox, 1, 4);

        // End Time
        endTimeLabel = new Label("End Time (HH:mm):");
        endHourSpinner = new Spinner<>(0, 23, 18);
        endMinSpinner = new Spinner<>(0, 59, 0);
        endTimeBox = new VBox(5, endHourSpinner, endMinSpinner);
        grid.add(endTimeLabel, 0, 5);
        grid.add(endTimeBox, 1, 5);

        statusLabel = new Label("Status: Stopped");
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        grid.add(statusLabel, 0, 6, 2, 1);
        GridPane.setHalignment(statusLabel, HPos.CENTER);

        toggleButton = new Button("Start");
        toggleButton.setMaxWidth(Double.MAX_VALUE);
        grid.add(toggleButton, 0, 7, 2, 1);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(6);
        grid.add(logArea, 0, 8, 2, 1);

        setupEngineCallbacks();

        modeComboBox.setOnAction(e -> updateModeVisibility());

        toggleButton.setOnAction(e -> toggleJiggler(idleTimeSpinner, moveIntervalSpinner));

        Scene scene = new Scene(grid);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Initial update after showing to ensure correct initial size
        updateModeVisibility();
    }

    private void setupEngineCallbacks() {
        jigglerEngine.setCallbacks(
                status -> statusLabel.setText(status),
                message -> logArea.appendText(message + "\n"),
                () -> {
                    toggleButton.setText("Start");
                    statusLabel.setText("Status: Stopped");
                }
        );
    }

    private void updateModeVisibility() {
        JigglerMode selectedMode = modeComboBox.getValue();
        boolean isDurationMode = JigglerMode.FOR_DURATION == selectedMode;
        boolean isBetweenHoursMode = JigglerMode.BETWEEN_HOURS == selectedMode;

        durationLabel.setVisible(isDurationMode);
        durationLabel.setManaged(isDurationMode);
        durationSpinner.setVisible(isDurationMode);
        durationSpinner.setManaged(isDurationMode);

        startTimeLabel.setVisible(isBetweenHoursMode);
        startTimeLabel.setManaged(isBetweenHoursMode);
        startTimeBox.setVisible(isBetweenHoursMode);
        startTimeBox.setManaged(isBetweenHoursMode);

        endTimeLabel.setVisible(isBetweenHoursMode);
        endTimeLabel.setManaged(isBetweenHoursMode);
        endTimeBox.setVisible(isBetweenHoursMode);
        endTimeBox.setManaged(isBetweenHoursMode);

        if (primaryStage != null && primaryStage.getScene() != null) {
            primaryStage.sizeToScene();
        }
    }

    private void toggleJiggler(Spinner<Integer> idleTimeSpinner, Spinner<Integer> moveIntervalSpinner) {
        if (jigglerEngine.isRunning()) {
            jigglerEngine.stop();
        } else {
            if (JigglerMode.BETWEEN_HOURS == modeComboBox.getValue() && !validateTimeInputs()) {
                return;
            }

            jigglerEngine.setConfig(
                    idleTimeSpinner.getValue(),
                    moveIntervalSpinner.getValue(),
                    modeComboBox.getValue(),
                    durationSpinner.getValue(),
                    LocalTime.of(startHourSpinner.getValue(), startMinSpinner.getValue()),
                    LocalTime.of(endHourSpinner.getValue(), endMinSpinner.getValue())
            );

            logArea.clear();
            toggleButton.setText("Stop");
            jigglerEngine.start();
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
}