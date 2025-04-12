package client;

import shared.MorpionInterface;
import javax.swing.*;
import java.awt.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MorpionClientGUI extends JFrame {
    private MorpionInterface game;
    private String playerName;
    private String playerSymbol;
    private JButton[][] buttons = new JButton[3][3];
    private JLabel statusLabel;
    private JPanel mainPanel;
    private Color bgColor = new Color(240, 240, 240);
    private Color buttonColor = new Color(255, 255, 255);
    private Color xColor = new Color(44, 62, 80);
    private Color oColor = new Color(231, 76, 60);

    public MorpionClientGUI() {
        setupGUI();
        connectToServer();
    }

    private void setupGUI() {
        setTitle("Tic-Tac-Toe (RMI)");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(bgColor);

        // Status label
        statusLabel = new JLabel("Connecting to server...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(52, 152, 219));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Game board
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        boardPanel.setBackground(bgColor);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        Font buttonFont = new Font("Segoe UI", Font.BOLD, 60);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int row = i, col = j;
                buttons[i][j] = new JButton();
                buttons[i][j].setFont(buttonFont);
                buttons[i][j].setBackground(buttonColor);
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].addActionListener(e -> handleMove(row, col));
                buttons[i][j].setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 2));
                boardPanel.add(buttons[i][j]);
            }
        }

        mainPanel.add(statusLabel, BorderLayout.NORTH);
        mainPanel.add(boardPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    private void connectToServer() {
        try {
            // Get player name with a modern input dialog
            playerName = JOptionPane.showInputDialog(this,
                    "Enter your name:",
                    "Player Registration",
                    JOptionPane.PLAIN_MESSAGE);

            if (playerName == null || playerName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name cannot be empty. Exiting...");
                System.exit(0);
            }

            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            game = (MorpionInterface) registry.lookup("MorpionGame");

            String status = game.registerPlayer(playerName);
            updateStatus(status.equals("WAIT") ? "Waiting for opponent..." : "Connected as Player O");

            new Thread(this::gameLoop).start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to connect to server: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void handleMove(int row, int col) {
        try {
            String result = game.makeMove(row, col, playerName);
            if (result.equals("VALID_MOVE")) {
                updateBoard();
            } else {
                showMessage(result);
            }
        } catch (Exception e) {
            showMessage("Error: " + e.getMessage());
        }
    }

    private void gameLoop() {
        try {
            // Wait for game to start
            while (!game.isGameReady()) {
                Thread.sleep(1000);
            }

            playerSymbol = game.getPlayerSymbol(playerName);
            updateStatus("Game started! You are Player " + playerSymbol);

            // Main game loop
            while (!game.isGameOver()) {
                updateBoard();
                Thread.sleep(500);
            }

            // Game over
            updateBoard();
            String winner = game.getWinner();
            if (winner.equals("Draw")) {
                showMessage("Game ended in a draw!");
            } else {
                showMessage("Player " + winner + " wins!");
            }

            // Ask to replay
            int choice = JOptionPane.showConfirmDialog(this,
                    "Would you like to play again?",
                    "Game Over",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                game.resetGame();
                gameLoop();
            } else {
                game.disconnectPlayer(playerName);
                System.exit(0);
            }

        } catch (Exception e) {
            showMessage("Game error: " + e.getMessage());
        }
    }

    private void updateBoard() throws Exception {
        String boardState = game.getCurrentBoard();
        String[] rows = boardState.split("\n");

        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 3; i++) {
                String[] cells = rows[i * 2].split("\\|");
                for (int j = 0; j < 3; j++) {
                    String cell = cells[j].trim();
                    buttons[i][j].setText(cell);
                    if (cell.equals("X")) {
                        buttons[i][j].setForeground(xColor);
                    } else if (cell.equals("O")) {
                        buttons[i][j].setForeground(oColor);
                    }
                }
            }

            try {
                if (game.isPlayerTurn(playerName)) {
                    updateStatus("Your turn (Player " + playerSymbol + ")");
                } else {
                    updateStatus("Waiting for opponent...");
                }
            } catch (Exception e) {
                updateStatus("Error: " + e.getMessage());
            }
        });
    }

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
        });
    }

    private void showMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message);
        });
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