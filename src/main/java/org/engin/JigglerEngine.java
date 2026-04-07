package org.engin;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.robot.Robot;

import java.time.LocalTime;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JigglerEngine {
    private static final Logger logger = Logger.getLogger(JigglerEngine.class.getName());
    private final Robot robot;
    private final Random random = new Random();
    private volatile boolean running = false;

    private int idleTimeMinutes = 3;
    private int moveIntervalSeconds = 5;
    private JigglerMode mode = JigglerMode.INFINITE;
    private int durationHours = 1;
    private LocalTime startTime = LocalTime.of(9, 0);
    private LocalTime endTime = LocalTime.of(18, 0);

    private Consumer<String> statusUpdate;
    private Consumer<String> logUpdate;
    private Runnable onStop;

    public JigglerEngine() {
        this.robot = new Robot();
    }

    public void setConfig(int idleTimeMinutes, int moveIntervalSeconds, JigglerMode mode,
                          int durationHours, LocalTime startTime, LocalTime endTime) {
        this.idleTimeMinutes = idleTimeMinutes;
        this.moveIntervalSeconds = moveIntervalSeconds;
        this.mode = mode;
        this.durationHours = durationHours;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void setCallbacks(Consumer<String> statusUpdate, Consumer<String> logUpdate, Runnable onStop) {
        this.statusUpdate = statusUpdate;
        this.logUpdate = logUpdate;
        this.onStop = onStop;
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        if (running) return;
        running = true;

        log("Mouse Jiggler started.");
        updateStatus("Status: Running");

        Thread.ofVirtual().start(this::runLoop);
    }

    public void stop() {
        running = false;
        updateStatus("Status: Stopped");
        log("Mouse Jiggler stopped.");
        if (onStop != null) {
            Platform.runLater(onStop);
        }
    }

    private void runLoop() {
        try {
            long startTimeMillis = System.currentTimeMillis();
            long durationMillis = durationHours * 3600 * 1000L;

            while (running) {
                if (mode == JigglerMode.FOR_DURATION && System.currentTimeMillis() - startTimeMillis >= durationMillis) {
                    log("Duration reached.");
                    break;
                } else if (mode == JigglerMode.BETWEEN_HOURS) {
                    LocalTime now = LocalTime.now();
                    if (now.isBefore(startTime) || now.isAfter(endTime)) {
                        log("Outside of scheduled hours.");
                        break;
                    }
                }

                Point2D currentPos = getMousePosition();
                Thread.sleep(moveIntervalSeconds * 1000L);
                Point2D newPos = getMousePosition();

                if (!currentPos.equals(newPos)) {
                    updateStatus("Status: Paused");
                    log("Mouse movement detected. Pausing for " + idleTimeMinutes + " minutes...");
                    Thread.sleep(idleTimeMinutes * 60 * 1000L);
                    updateStatus("Status: Running");
                    continue;
                }

                // Per instructions: 1-5 pixels range
                int dx = random.nextInt(11) - 5; // -5 to 5
                int dy = random.nextInt(11) - 5; // -5 to 5

                // Ensure at least 1 pixel movement if both are 0
                if (dx == 0 && dy == 0) dx = 1;

                mouseMove(currentPos.getX() + dx, currentPos.getY() + dy);
                log("Mouse moved at " + LocalTime.now().withNano(0));
            }
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Jiggler thread interrupted", e);
        } finally {
            stop();
        }
    }

    private Point2D getMousePosition() {
        CompletableFuture<Point2D> future = new CompletableFuture<>();
        Platform.runLater(() -> future.complete(robot.getMousePosition()));
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Error getting mouse position", e);
            return Point2D.ZERO;
        }
    }

    private void mouseMove(double x, double y) {
        Platform.runLater(() -> robot.mouseMove(x, y));
    }

    private void log(String message) {
        if (logUpdate != null) {
            Platform.runLater(() -> logUpdate.accept(message));
        }
    }

    private void updateStatus(String status) {
        if (statusUpdate != null) {
            Platform.runLater(() -> statusUpdate.accept(status));
        }
    }
}
