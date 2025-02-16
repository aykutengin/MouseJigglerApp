import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.util.Random;
import java.util.logging.*;

public class MouseJigglerApp {
    private static volatile boolean running = false;
    private static Robot robot;
    private static final Logger logger = Logger.getLogger(MouseJigglerApp.class.getName());
    private static Timer idleTimer;
    private static int idleTimeMinutes = 3;
    private static int moveIntervalSeconds = 5;
    private static final Random random = new Random();
    private static JLabel statusLabel;
    private static JTextArea logArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MouseJigglerApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Mouse Jiggler");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new GridBagLayout());
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel idleTimeLabel = new JLabel("Idle Time (min):");
        gbc.gridx = 0;
        gbc.gridy = 0;
        frame.add(idleTimeLabel, gbc);

        JSpinner idleTimeSpinner = new JSpinner(new SpinnerNumberModel(idleTimeMinutes, 1, 60, 1));
        gbc.gridx = 1;
        frame.add(idleTimeSpinner, gbc);

        JLabel moveIntervalLabel = new JLabel("Move Interval (sec):");
        gbc.gridx = 0;
        gbc.gridy = 1;
        frame.add(moveIntervalLabel, gbc);

        JSpinner moveIntervalSpinner = new JSpinner(new SpinnerNumberModel(moveIntervalSeconds, 1, 60, 1));
        gbc.gridx = 1;
        frame.add(moveIntervalSpinner, gbc);

        JButton toggleButton = new JButton("Start");
        toggleButton.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        frame.add(toggleButton, gbc);

        statusLabel = new JLabel("Status: Stopped", SwingConstants.CENTER);
        gbc.gridy = 3;
        frame.add(statusLabel, gbc);

        logArea = new JTextArea(6, 30);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        frame.add(scrollPane, gbc);

        setupLogger();
        toggleButton.addActionListener(e -> toggleJiggler(toggleButton, idleTimeSpinner, moveIntervalSpinner));
        frame.setVisible(true);
    }

    private static void setupLogger() {
        logger.setUseParentHandlers(false);
        StreamHandler handler = new StreamHandler() {
            @Override
            public void publish(LogRecord logRecord) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(logRecord.getMessage() + "\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength()); // Auto-scroll
                });
                flush();
            }
        };
        handler.setFormatter(new SimpleFormatter());
        logger.addHandler(handler);
    }

    private static void toggleJiggler(JButton button, JSpinner idleTimeSpinner, JSpinner moveIntervalSpinner) {
        button.setEnabled(false); // Prevent rapid toggling
        SwingUtilities.invokeLater(() -> button.setEnabled(true));

        if (running) {
            stopJiggler(button);
        } else {
            idleTimeMinutes = (int) idleTimeSpinner.getValue();
            moveIntervalSeconds = (int) moveIntervalSpinner.getValue();
            logger.info(String.format("Idle time set to %d minutes.", idleTimeMinutes));
            logger.info(String.format("Move interval set to %d seconds.", moveIntervalSeconds));
            startJiggler(button);
        }
    }

    private static void startJiggler(JButton button) {
        running = true;
        button.setText("Stop");
        statusLabel.setText("Status: Running");
        logArea.setText(""); // Clearing logArea
        logger.info("Mouse Jiggler started.");
        idleTimer = new Timer(idleTimeMinutes * 60 * 100, e -> {
            statusLabel.setText("Status: Running");
            logger.info("Idle time over, resuming mouse jiggling.");
        });
        idleTimer.setRepeats(false);
        Thread jigglerThread = new Thread(() -> {
            try {
                robot = new Robot();
                while (running) {
                    Point currentMouseLocation = MouseInfo.getPointerInfo().getLocation();
                    Thread.sleep(moveIntervalSeconds * 1000L);
                    Point newMouseLocation = MouseInfo.getPointerInfo().getLocation();

                    if (!currentMouseLocation.equals(newMouseLocation)) {
                        statusLabel.setText("Status: Paused");
                        logger.info(String.format("Mouse movement detected. Pausing for %s minutes.", idleTimeMinutes));
                        idleTimer.restart();
                        while (idleTimer.isRunning())
                            Thread.sleep(moveIntervalSeconds * 1000L);
                        continue;
                    }

                    int xOffset = random.nextInt(10) - 5;
                    int yOffset = random.nextInt(10) - 5;
                    robot.mouseMove(currentMouseLocation.x + xOffset, currentMouseLocation.y + yOffset);
                    logger.info("Mouse moved slightly at " + LocalTime.now().withNano(0));
                }
            } catch (AWTException e) {
                logger.log(Level.SEVERE, "Error in Mouse Jiggler", e);
            } catch (InterruptedException e) {
                logger.info("Jiggler thread interrupted.");
                Thread.currentThread().interrupt();
            } finally {
                running = false;
                SwingUtilities.invokeLater(() -> stopJiggler(button));
            }
        });
        jigglerThread.start();
    }

    private static void stopJiggler(JButton button) {
        if (idleTimer != null)
            idleTimer.stop();
        running = false;
        button.setText("Start");
        statusLabel.setText("Status: Stopped");
        logger.info("Mouse Jiggler stopped.");
    }
}
