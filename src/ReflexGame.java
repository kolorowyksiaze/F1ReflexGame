package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class ReflexGame extends JFrame {
    private JPanel panel;
    private JLabel infoLabel;
    private Random random;
    private int redLights = 5;
    private int currentLight = 0;
    private boolean greenShown = false;
    private long greenTime = 0;
    private long reactionTime = 0;
    private JLabel[] lights;
    private JButton restartButton;
    private JButton stopButton;
    private JButton startButton;
    private JPanel startPanel;
    private boolean roundOver = false;
    private int round = 0;
    private final int totalRounds = 10;
    private java.util.List<Long> times = new ArrayList<>();
    private javax.swing.Timer roundTimer = null;
    private javax.swing.Timer lightsOffTimer = null;
    private boolean stopped = false;
    private JPanel statsPanel;
    private JScrollPane statsScrollPane;
    private JLabel summaryLabel;
    private int falseStarts = 0;
    private JButton saveButton;

    public ReflexGame() {
        setTitle("F1 Reflex Game");
        setSize(600, 350);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        panel = new JPanel(null);
        infoLabel = new JLabel("Wait for green, then click anywhere!");
        infoLabel.setBounds(10, 10, 500, 30);
        panel.add(infoLabel);
        add(panel);
        random = new Random();
        lights = new JLabel[redLights];
        for (int i = 0; i < redLights; i++) {
            lights[i] = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillOval(0, 0, getWidth(), getHeight());
                }
            };
            lights[i].setOpaque(false);
            lights[i].setBackground(Color.DARK_GRAY);
            lights[i].setBounds(60 + i * 100, 80, 60, 60);
            panel.add(lights[i]);
        }
        // Hide lights at the start
        for (JLabel light : lights) {
            light.setVisible(false);
        }
        // Add start panel with instructions and start button
        startPanel = new JPanel(null);
        startPanel.setBounds(0, 0, 600, 350);
        JLabel instructions = new JLabel("<html><center>Welcome to the F1 Reflex Game!<br>Instructions:<br>1. Wait for the red lights to appear and then turn green.<br>2. Click anywhere in the window as soon as you see green.<br>3. Try to be as fast as possible!<br>4. If you click too soon, you'll have to repeat the round.<br>5. You can stop at any time to see your stats.<br><br>Click 'Start' to begin.</center></html>");
        instructions.setBounds(50, 30, 500, 180);
        instructions.setHorizontalAlignment(SwingConstants.CENTER);
        startPanel.add(instructions);
        startButton = new JButton("Start");
        startButton.setBounds(440, 280, 150, 30); // bottom right
        startButton.addActionListener(e -> {
            startPanel.setVisible(false);
            for (JLabel light : lights) light.setVisible(true);
            startSequence();
        });
        startPanel.add(startButton);
        panel.add(startPanel);
        // Add stats panel (hidden by default) - now at the top
        statsPanel = new JPanel();
        statsPanel.setLayout(new BorderLayout());
        statsPanel.setBounds(10, 40, 580, 260); // increased height for table
        statsPanel.setVisible(false);
        statsScrollPane = new JScrollPane();
        statsPanel.add(statsScrollPane, BorderLayout.CENTER);
        summaryLabel = new JLabel();
        summaryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statsPanel.add(summaryLabel, BorderLayout.SOUTH);
        panel.add(statsPanel);
        restartButton = new JButton("Restart");
        restartButton.setBounds(440, 280, 150, 30); // bottom right
        restartButton.setVisible(false);
        restartButton.addActionListener(e -> restartGame());
        panel.add(restartButton);
        stopButton = new JButton("Stop & Show Stats");
        stopButton.setBounds(440, 280, 150, 30); // left of restart
        stopButton.setVisible(true);
        stopButton.addActionListener(e -> stopAndShowStats());
        panel.add(stopButton);
        saveButton = new JButton("Save Results");
        saveButton.setBounds(440, 248, 150, 30); // left of restart
        saveButton.setVisible(false);
        saveButton.addActionListener(e -> saveResultsToJson());
        panel.add(saveButton);
        panel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                handleClick();
            }
        });
        // Do NOT call startSequence() here; wait for Start button
        startPanel.setVisible(true);
    }

    private void startSequence() {
        if (stopped) return;
        for (JLabel light : lights) light.setVisible(true);
        infoLabel.setText("Round " + (round + 1) + "/" + totalRounds + ": Wait for green, then click anywhere!");
        for (JLabel light : lights) {
            light.setBackground(Color.DARK_GRAY);
        }
        currentLight = 0;
        greenShown = false;
        reactionTime = 0;
        roundOver = false;
        restartButton.setVisible(false);
        stopButton.setVisible(true);
        statsPanel.setVisible(false);
        if (roundTimer != null) roundTimer.stop();
        roundTimer = new javax.swing.Timer(500, null);
        roundTimer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (stopped) { roundTimer.stop(); return; }
                if (currentLight < redLights) {
                    lights[currentLight].setBackground(Color.RED);
                    currentLight++;
                } else {
                    roundTimer.stop();
                    showGreenAfterRandomDelay();
                }
            }
        });
        roundTimer.start();
    }

    private void showGreenAfterRandomDelay() {
        if (stopped) return;
        int delay = 1000 + random.nextInt(1500); // 1-2.5 seconds
        if (roundTimer != null) roundTimer.stop();
        roundTimer = new javax.swing.Timer(delay, e -> showGreen());
        roundTimer.setRepeats(false);
        roundTimer.start();
    }

    private void showGreen() {
        if (stopped) return;
        Color darkGreen = new Color(0, 128, 0);
        for (JLabel light : lights) {
            light.setBackground(darkGreen);
        }
        greenShown = true;
        greenTime = System.currentTimeMillis();
        infoLabel.setText("GO! Click now!");
    }

    private void handleClick() {
        if (roundOver || stopped) return;
        if (!greenShown) {
            infoLabel.setText("Too soon! Wait for green! Try again.");
            roundOver = true;
            stopButton.setVisible(false);
            if (roundTimer != null) roundTimer.stop();
            falseStarts++;
            // Give user another chance for this round after a short delay
            javax.swing.Timer retryTimer = new javax.swing.Timer(1200, e -> startSequence());
            retryTimer.setRepeats(false);
            retryTimer.start();
        } else if (reactionTime == 0) {
            reactionTime = System.currentTimeMillis() - greenTime;
            times.add(reactionTime);
            infoLabel.setText("Reaction time: " + reactionTime + " ms");
            roundOver = true;
            stopButton.setVisible(false);
            // F1 lights round: turn off lights after green and click
            if (lightsOffTimer != null) lightsOffTimer.stop();
            lightsOffTimer = new javax.swing.Timer(500, e -> turnOffLights());
            lightsOffTimer.setRepeats(false);
            lightsOffTimer.start();
            if (round + 1 < totalRounds) {
                round++;
                if (roundTimer != null) roundTimer.stop();
                roundTimer = new javax.swing.Timer(1200, e -> startSequence());
                roundTimer.setRepeats(false);
                roundTimer.start();
            } else {
                if (roundTimer != null) roundTimer.stop();
                roundTimer = new javax.swing.Timer(1200, e -> showStats());
                roundTimer.setRepeats(false);
                roundTimer.start();
            }
        }
    }

    private void turnOffLights() {
        for (JLabel light : lights) {
            light.setBackground(Color.DARK_GRAY);
            light.setVisible(true);
        }
        statsPanel.setVisible(false);
    }

    private void stopAndShowStats() {
        stopped = true;
        if (roundTimer != null) roundTimer.stop();
        if (lightsOffTimer != null) lightsOffTimer.stop();
        showStats();
    }

    private void showStats() {
        stopButton.setVisible(false);
        restartButton.setVisible(true);
        restartButton.setBounds(440, 280, 150, 30); // bottom right
        panel.setComponentZOrder(restartButton, 0); // bring restart button to front
        saveButton.setVisible(true);
        saveButton.setBounds(440, 248, 150, 30);
        panel.setComponentZOrder(saveButton, 0);
        // Hide lights
        for (JLabel light : lights) {
            light.setVisible(false);
        }
        // Prepare stats panel
        statsPanel.setVisible(true);
        // Table for times
        String[] columnNames = {"Round", "Time (ms)"};
        String[][] data = new String[times.size()][2];
        for (int i = 0; i < times.size(); i++) {
            data[i][0] = String.valueOf(i + 1);
            data[i][1] = String.valueOf(times.get(i));
        }
        JTable table = new JTable(data, columnNames);
        table.setEnabled(false);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);
        statsScrollPane.setViewportView(table);
        // Summary and advanced stats
        long sum = 0;
        long best = Long.MAX_VALUE, worst = Long.MIN_VALUE;
        ArrayList<Long> sorted = new ArrayList<>(times);
        Collections.sort(sorted);
        for (long t : times) {
            sum += t;
            if (t < best) best = t;
            if (t > worst) worst = t;
        }
        double avg = sum / (double) times.size();
        double median = sorted.size() % 2 == 0 ?
            (sorted.get(sorted.size()/2-1) + sorted.get(sorted.size()/2))/2.0 :
            sorted.get(sorted.size()/2);
        // Standard deviation
        double variance = 0;
        for (long t : times) variance += (t - avg) * (t - avg);
        variance /= times.size();
        double stddev = Math.sqrt(variance);
        // Consistency
        long consistency = worst - best;
        // Quartiles
        double q1 = sorted.size() > 1 ? sorted.get(sorted.size()/4) : avg;
        double q3 = sorted.size() > 1 ? sorted.get(3*sorted.size()/4) : avg;
        String summary = String.format(
            "<html>Avg: %.2f ms<br>Median: %.2f ms<br>StdDev: %.2f ms<br>Best: %d ms<br>Worst: %d ms<br>Consistency: %d ms<br>Q1: %.2f ms<br>Q3: %.2f ms<br>False starts: %d<br><br>Click Restart to play again.</html>",
            avg, median, stddev, best, worst, consistency, q1, q3, falseStarts);
        summaryLabel.setText(summary);
        infoLabel.setText("");
    }

    private void restartGame() {
        times.clear();
        round = 0;
        stopped = false;
        falseStarts = 0;
        for (JLabel light : lights) {
            light.setVisible(false);
        }
        statsPanel.setVisible(false);
        startPanel.setVisible(true);
        stopButton.setVisible(true);
        stopButton.setBounds(440, 280, 150, 30);
        restartButton.setVisible(false);
        restartButton.setBounds(440, 280, 150, 30);
        saveButton.setVisible(false);
    }

    private void saveResultsToJson() {
        String nick = JOptionPane.showInputDialog(this, "Enter your nickname (filename):", "Save Results", JOptionPane.PLAIN_MESSAGE);
        if (nick == null || nick.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nickname cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("nickname", nick);
            json.put("times", new ArrayList<>(times));
            long sum = 0;
            long best = Long.MAX_VALUE, worst = Long.MIN_VALUE;
            ArrayList<Long> sorted = new ArrayList<>(times);
            Collections.sort(sorted);
            for (long t : times) {
                sum += t;
                if (t < best) best = t;
                if (t > worst) worst = t;
            }
            double avg = sum / (double) times.size();
            double median = sorted.size() % 2 == 0 ?
                (sorted.get(sorted.size()/2-1) + sorted.get(sorted.size()/2))/2.0 :
                sorted.get(sorted.size()/2);
            double variance = 0;
            for (long t : times) variance += (t - avg) * (t - avg);
            variance /= times.size();
            double stddev = Math.sqrt(variance);
            long consistency = worst - best;
            double q1 = sorted.size() > 1 ? sorted.get(sorted.size()/4) : avg;
            double q3 = sorted.size() > 1 ? sorted.get(3*sorted.size()/4) : avg;
            json.put("avg", avg);
            json.put("median", median);
            json.put("stddev", stddev);
            json.put("best", best);
            json.put("worst", worst);
            json.put("consistency", consistency);
            json.put("q1", q1);
            json.put("q3", q3);
            json.put("falseStarts", falseStarts);
            // Write to file
            String filename = nick.trim() + ".json";
            try (java.io.FileWriter fw = new java.io.FileWriter(filename)) {
                fw.write(toPrettyJson(json));
            }
            JOptionPane.showMessageDialog(this, "Results saved to " + filename, "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving results: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String toPrettyJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append("  \"").append(entry.getKey()).append("\": ");
            if (entry.getValue() instanceof String) {
                sb.append("\"").append(entry.getValue()).append("\"");
            } else if (entry.getValue() instanceof java.util.List) {
                sb.append(entry.getValue().toString().replaceAll("([0-9]+)", "$1"));
            } else {
                sb.append(entry.getValue());
            }
            sb.append(",\n");
        }
        if (sb.lastIndexOf(",\n") == sb.length() - 2) sb.setLength(sb.length() - 2);
        sb.append("\n}");
        return sb.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ReflexGame().setVisible(true));
    }
}
