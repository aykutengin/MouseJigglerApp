import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.logging.*;

public class MouseJigglerApp {

    private static final String DATE_FORMAT_PATTERN = "HH:mm";
    private static final String BETWEEN_HOURS = "Between Hours";
    private static final String FOR_DURATION = "For Duration";
    private static volatile boolean running = false;
    private static Robot robot;
    private static final Logger logger = Logger.getLogger(MouseJigglerApp.class.getName());
    private static Timer idleTimer;
    private static int idleTimeMinutes = 3;
    private static int moveIntervalSeconds = 5;
    private static final Random random = new Random();
    private static JLabel statusLabel;
    private static JTextArea logArea;
    private static JComboBox<String> modeComboBox;
    private static JSpinner durationSpinner;
    private static JSpinner startHourSpinner;
    private static JSpinner endHourSpinner;
    private static JLabel durationLabel;
    private static JLabel startHourLabel;
    private static JLabel endHourLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MouseJigglerApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Mouse Jiggler");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(400, 350);
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

        JLabel modeLabel = new JLabel("Mode:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        frame.add(modeLabel, gbc);

        modeComboBox = new JComboBox<>(new String[]{"Infinite", FOR_DURATION, BETWEEN_HOURS});
        gbc.gridx = 1;
        frame.add(modeComboBox, gbc);

        durationLabel = new JLabel("Duration (hours):");
        gbc.gridx = 0;
        gbc.gridy = 3;
        frame.add(durationLabel, gbc);

        durationSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 24, 1));
        gbc.gridx = 1;
        frame.add(durationSpinner, gbc);

        startHourLabel = new JLabel("Start Time:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        frame.add(startHourLabel, gbc);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startTime = calendar.getTime();
        startHourSpinner = new JSpinner(new SpinnerDateModel(startTime, null, null, Calendar.HOUR_OF_DAY));
        JSpinner.DateEditor startEditor = new JSpinner.DateEditor(startHourSpinner, DATE_FORMAT_PATTERN);
        startHourSpinner.setEditor(startEditor);
        gbc.gridx = 1;
        frame.add(startHourSpinner, gbc);

        endHourLabel = new JLabel("End Time:");
        gbc.gridx = 0;
        gbc.gridy = 5;
        frame.add(endHourLabel, gbc);

        calendar.set(Calendar.HOUR_OF_DAY, 18);
        Date endTime = calendar.getTime();
        endHourSpinner = new JSpinner(new SpinnerDateModel(endTime, null, null, Calendar.HOUR_OF_DAY));
        JSpinner.DateEditor endEditor = new JSpinner.DateEditor(endHourSpinner, DATE_FORMAT_PATTERN);
        endHourSpinner.setEditor(endEditor);
        gbc.gridx = 1;
        frame.add(endHourSpinner, gbc);

        JButton toggleButton = new JButton("Start");
        toggleButton.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        frame.add(toggleButton, gbc);

        statusLabel = new JLabel("Status: Stopped", SwingConstants.CENTER);
        gbc.gridy = 8;
        frame.add(statusLabel, gbc);

        logArea = new JTextArea(6, 30);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        frame.add(scrollPane, gbc);

        setupLogger();
        toggleButton.addActionListener(e -> toggleJiggler(toggleButton, idleTimeSpinner, moveIntervalSpinner));
        modeComboBox.addActionListener(e -> updateModeVisibility());
        updateModeVisibility(); // Initial call to set visibility based on default selection
        frame.setVisible(true);
    }

    private static void updateModeVisibility() {
        String selectedMode = (String) modeComboBox.getSelectedItem();
        boolean isDurationMode = FOR_DURATION.equals(selectedMode);
        boolean isBetweenHoursMode = BETWEEN_HOURS.equals(selectedMode);

        durationLabel.setVisible(isDurationMode);
        durationSpinner.setVisible(isDurationMode);
        startHourLabel.setVisible(isBetweenHoursMode);
        startHourSpinner.setVisible(isBetweenHoursMode);
        endHourLabel.setVisible(isBetweenHoursMode);
        endHourSpinner.setVisible(isBetweenHoursMode);
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
            if (!validateTimeInputs()) {
                return;
            }
            idleTimeMinutes = (int) idleTimeSpinner.getValue();
            moveIntervalSeconds = (int) moveIntervalSpinner.getValue();
            logger.info(String.format("Idle time set to %d minutes.", idleTimeMinutes));
            logger.info(String.format("Move interval set to %d seconds.", moveIntervalSeconds));
            startJiggler(button);
        }
    }

    private static boolean validateTimeInputs() {
        Date startHour = (Date) startHourSpinner.getValue();
        Date endHour = (Date) endHourSpinner.getValue();

        if (startHour.after(endHour)) {
            JOptionPane.showMessageDialog(null, "Start time must be before end time.", "Invalid Time Input", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private static void startJiggler(JButton button) {
        running = true;
        button.setText("Stop");
        statusLabel.setText("Status: Running");
        logArea.setText(""); // Clearing logArea
        logger.info("Mouse Jiggler started.");
        idleTimer = new Timer(idleTimeMinutes * 60 * 1000, e -> {
            statusLabel.setText("Status: Running");
            logger.info("Idle time over, resuming mouse jiggling.");
        });
        idleTimer.setRepeats(false);
        Thread.ofVirtual().start(() -> {
            try {
                robot = new Robot();
                long startTime = System.currentTimeMillis();
                long durationMillis = (int) durationSpinner.getValue() * 3600 * 1000L;
                Date startDate = ((Date) startHourSpinner.getValue());
                Date endDate = ((Date) endHourSpinner.getValue());

                while (running) {
                    if (modeComboBox.getSelectedItem().equals(FOR_DURATION) && System.currentTimeMillis() - startTime >= durationMillis) {
                        logger.info("The specified duration has been exceeded.");
                        break;
                    } else if (modeComboBox.getSelectedItem().equals(BETWEEN_HOURS)) {
                        Date currentDate = new Date(System.currentTimeMillis());
                        logger.finest("currentDate: " + currentDate + ", startDate: " + startDate + ", endDate: " + endDate);
                        if ((currentDate.before(startDate)) || (currentDate.after(endDate))) {
                            logger.info("The time is out of hours range");
                            break;
                        }
                    }

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
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error parsing time", e);
            } finally {
                running = false;
                SwingUtilities.invokeLater(() -> stopJiggler(button));
            }
        });
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