package client;

import shared.MorpionInterface;
import shared.MorpionCallback;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;

public class MorpionClientGUI extends JFrame implements MorpionCallback {
    private MorpionInterface game;
    private String playerName;
    private String playerSymbol;
    private JButton[][] buttons = new JButton[3][3];
    private JLabel statusLabel;
    private JLabel playerInfoLabel;
    private JLabel statsLabel;
    private JPanel mainPanel;
    private JPanel historyPanel;
    private JTextArea historyArea;

    // Color scheme
    private final Color bgColor = new Color(245, 247, 250);
    private final Color cardColor = new Color(255, 255, 255);
    private final Color primaryColor = new Color(70, 130, 180);
    private final Color secondaryColor = new Color(231, 76, 60);
    private final Color accentColor = new Color(46, 204, 113);
    private final Color textColor = new Color(51, 51, 51);

    public MorpionClientGUI() {
        setupGUI();
        connectToServer();
    }

    private void setupGUI() {
        setTitle("Morpion Battle");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(bgColor);

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(bgColor);

        statusLabel = new JLabel("Connecting to server...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16)); // Smaller font
        statusLabel.setOpaque(true);
        statusLabel.setBackground(primaryColor);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        playerInfoLabel = new JLabel("", SwingConstants.CENTER);
        playerInfoLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        playerInfoLabel.setForeground(textColor);

        statsLabel = new JLabel("", SwingConstants.CENTER);
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statsLabel.setForeground(textColor);

        headerPanel.add(statusLabel, BorderLayout.NORTH);
        headerPanel.add(playerInfoLabel, BorderLayout.CENTER);
        headerPanel.add(statsLabel, BorderLayout.SOUTH);

        // Game board
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 5, 5)); // Reduced gaps
        boardPanel.setBackground(bgColor);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Font buttonFont = new Font("Segoe UI", Font.BOLD, 48); // Smaller font
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int row = i, col = j;
                buttons[i][j] = new JButton();
                buttons[i][j].setFont(buttonFont);
                buttons[i][j].setBackground(cardColor);
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].setPreferredSize(new Dimension(80, 80)); // Fixed button size
                buttons[i][j].addActionListener(e -> handleMove(row, col));

                buttons[i][j].addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        if (buttons[row][col].getText().isEmpty()) {
                            buttons[row][col].setBackground(new Color(245, 245, 245));
                        }
                    }

                    public void mouseExited(MouseEvent e) {
                        buttons[row][col].setBackground(cardColor);
                    }
                });

                boardPanel.add(buttons[i][j]);
            }
        }

        // History panel - now with fixed size
        historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("Match History (Last 5 games)"));
        historyPanel.setBackground(bgColor);
        historyPanel.setPreferredSize(new Dimension(0, 120)); // Fixed height

        historyArea = new JTextArea(5, 20); // Fixed rows
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        historyArea.setBackground(bgColor);
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(historyArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        historyPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(boardPanel, BorderLayout.CENTER);
        mainPanel.add(historyPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void connectToServer() {
        try {
            playerName = JOptionPane.showInputDialog(this,
                    "Enter your name:", "Player Registration", JOptionPane.PLAIN_MESSAGE);

            if (playerName == null || playerName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name cannot be empty. Exiting...",
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }

            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            game = (MorpionInterface) registry.lookup("MorpionGame");

            MorpionCallback callbackStub = (MorpionCallback) UnicastRemoteObject.exportObject(this, 0);

            String status = game.registerPlayer(playerName, callbackStub);
            updateStatus(status.equals("WAIT") ? "Waiting for opponent..." : "Connected to game");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to server: " + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void handleMove(int row, int col) {
        try {
            if (game.isPlayerTurn(playerName)) {
                String result = game.makeMove(row, col, playerName);
                if (!result.equals("VALID_MOVE")) {
                    JOptionPane.showMessageDialog(this, result, "Move Error", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void updateBoard(String boardState) {
        SwingUtilities.invokeLater(() -> {
            String[] rows = boardState.split("\n");
            for (int i = 0; i < 3; i++) {
                String[] cells = rows[i * 2].split("\\|");
                for (int j = 0; j < 3; j++) {
                    String cell = cells[j].trim();
                    if (cell.equals("X")) {
                        buttons[i][j].setText("X");
                        buttons[i][j].setForeground(primaryColor);
                    } else if (cell.equals("O")) {
                        buttons[i][j].setText("O");
                        buttons[i][j].setForeground(secondaryColor);
                    } else {
                        buttons[i][j].setText("");
                    }
                }
            }

            try {
                if (game.isPlayerTurn(playerName)) {
                    statusLabel.setText("YOUR TURN - Player " + playerSymbol);
                    statusLabel.setBackground(accentColor);
                } else {
                    statusLabel.setText("Waiting for opponent's move...");
                    statusLabel.setBackground(primaryColor);
                }
                updateStats();
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(this, "Error updating game state", "Network Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    public void gameReady(String playerSymbol) {
        SwingUtilities.invokeLater(() -> {
            this.playerSymbol = playerSymbol;
            updatePlayerInfo();
            updateStatus("Game started! You are Player " + playerSymbol);
            try {
                updateStats();
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(this, "Error getting stats", "Network Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    public void gameOver(String winner) {
        SwingUtilities.invokeLater(() -> {
            try {
                String message;
                if ("DRAW".equals(winner)) {
                    message = "Game ended in a draw!";
                } else {
                    message = winner.equals(playerName) ? "You won!" : "You lost!";
                }

                int choice = JOptionPane.showOptionDialog(this,
                        message + "\nWould you like to play again?",
                        "Game Over",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new Object[] { "Play Again", "Quit" },
                        "Play Again");

                if (choice == JOptionPane.YES_OPTION) {
                    game.resetGame();
                    resetBoard();
                    playerSymbol = null;
                    updateStatus("Waiting for opponent...");
                } else {
                    game.disconnectPlayer(playerName);
                    System.exit(0);
                }
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(this, "Error resetting game", "Network Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    public void opponentDisconnected() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "Your opponent has disconnected. The game will be reset.",
                    "Opponent Disconnected",
                    JOptionPane.INFORMATION_MESSAGE);
            try {
                game.resetGame();
                resetBoard();
                playerSymbol = null;
                updateStatus("Waiting for new opponent...");
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(this, "Error resetting game", "Network Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void updateStats() throws RemoteException {
        Map<String, Integer> stats = game.getPlayerStats(playerName);
        String opponent = game.getOpponentName(playerName);

        String statsText = String.format("Wins: %d | Losses: %d | Draws: %d | Opponent: %s",
                stats.get("wins"), stats.get("losses"), stats.get("draws"),
                opponent != null ? opponent : "None");
        statsLabel.setText(statsText);

        List<String> history = game.getMatchHistory(playerName);
        historyArea.setText("");
        // Show only last 5 matches
        int startIndex = Math.max(0, history.size() - 5);
        for (int i = startIndex; i < history.size(); i++) {
            historyArea.append(history.get(i) + "\n");
        }
    }

    private void updatePlayerInfo() {
        playerInfoLabel.setText("Playing as: " + playerName + " (" + playerSymbol + ")");
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void resetBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setBackground(cardColor);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                MorpionClientGUI gui = new MorpionClientGUI();
                gui.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}