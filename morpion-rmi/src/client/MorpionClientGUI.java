package client;

import shared.MorpionInterface;
import shared.MorpionInterface.MoveStatus;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

public class MorpionClientGUI extends JFrame {
    private MorpionInterface game;
    private String playerName;
    private String playerSymbol;
    private JButton[][] buttons = new JButton[3][3];
    private JLabel statusLabel;
    private JLabel playerLabel;
    private JPanel mainPanel;
    private ExecutorService executor = Executors.newFixedThreadPool(2);
    private String currentRoomId;
    private boolean gameEnded = false;

    // Color scheme
    private final Color bgColor = new Color(245, 245, 245);
    private final Color buttonColor = new Color(255, 255, 255);
    private final Color hoverColor = new Color(230, 230, 230);
    private final Color xColor = new Color(44, 62, 80);
    private final Color oColor = new Color(231, 76, 60);
    private final Color yourTurnColor = new Color(46, 204, 113);
    private final Color waitingColor = new Color(52, 152, 219);
    private final Color gameOverColor = new Color(155, 89, 182);
    private final Color errorColor = new Color(231, 76, 60);

    public MorpionClientGUI() {
        setupGUI();
        connectToServer();
    }

    private void setupGUI() {
        setTitle("Tic-Tac-Toe (RMI)");
        setSize(550, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main panel
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(bgColor);

        // Info panel
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        infoPanel.setBackground(bgColor);

        playerLabel = new JLabel("", SwingConstants.CENTER);
        playerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        playerLabel.setForeground(new Color(44, 62, 80));

        statusLabel = new JLabel("Connecting to server...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(waitingColor);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        infoPanel.add(playerLabel);
        infoPanel.add(statusLabel);

        // Game board
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        boardPanel.setBackground(bgColor);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        Font buttonFont = new Font("Segoe UI", Font.BOLD, 72);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int row = i, col = j;
                buttons[i][j] = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (!getText().isEmpty()) {
                            Graphics2D g2 = (Graphics2D) g;
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(getText().equals("X") ? xColor : oColor);
                            FontMetrics fm = g2.getFontMetrics();
                            String text = getText();
                            int x = (getWidth() - fm.stringWidth(text)) / 2;
                            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                            g2.drawString(text, x, y);
                        }
                    }
                };
                buttons[i][j].setFont(buttonFont);
                buttons[i][j].setBackground(buttonColor);
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(189, 195, 199)), null));

                // Hover effects
                buttons[i][j].addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        if (buttons[row][col].getText().isEmpty()) {
                            buttons[row][col].setBackground(hoverColor);
                        }
                    }

                    public void mouseExited(MouseEvent e) {
                        if (buttons[row][col].getText().isEmpty()) {
                            buttons[row][col].setBackground(buttonColor);
                        }
                    }
                });

                buttons[i][j].addActionListener(e -> handleMove(row, col));
                boardPanel.add(buttons[i][j]);
            }
        }

        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(boardPanel, BorderLayout.CENTER);
        add(mainPanel);

        // Handle window closing
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
    }

    private void connectToServer() {
        executor.execute(() -> {
            try {
                // Initialize RMI connection
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                game = (MorpionInterface) registry.lookup("MorpionGame");

                playerName = JOptionPane.showInputDialog(this,
                        "Enter your name:",
                        "Player Registration",
                        JOptionPane.PLAIN_MESSAGE);

                if (playerName == null || playerName.trim().isEmpty()) {
                    shutdown();
                    return;
                }

                // Get available rooms first
                List<String> availableRooms = game.listAvailableRooms();

                // Create room selection dialog
                Object[] options;
                String message;
                if (availableRooms.isEmpty()) {
                    message = "<html><div style='text-align: center; width: 300px;'>" +
                            "<b>No available rooms found</b><br>" +
                            "Would you like to create a new room?</div></html>";
                    options = new Object[] { "Create Room", "Cancel" };
                } else {
                    message = "<html><div style='text-align: center; width: 300px;'>" +
                            "<b>Available Rooms:</b><br><br>" +
                            String.join("<br>", availableRooms) +
                            "<br><br>Join existing room or create new one?</div></html>";
                    options = new Object[] { "Join Room", "Create Room", "Cancel" };
                }

                int choice = JOptionPane.showOptionDialog(
                        this,
                        message,
                        "Room Selection",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);

                if (choice == 0) { // First button (Join or Create)
                    if (availableRooms.isEmpty()) {
                        // Create room if none available
                        currentRoomId = game.createRoom(playerName);
                        updateStatus("Room created! ID: " + currentRoomId);
                        playerLabel.setText("Player: " + playerName + " (Waiting for opponent)");
                    } else {
                        // Join selected room
                        String selectedRoom = (String) JOptionPane.showInputDialog(
                                this,
                                "Select room to join:",
                                "Join Room",
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                availableRooms.toArray(),
                                availableRooms.get(0));

                        if (selectedRoom == null) {
                            shutdown();
                            return;
                        }

                        currentRoomId = selectedRoom.split(" ")[1];
                        MorpionInterface.RoomStatus status = game.joinRoom(currentRoomId, playerName);
                        if (status != MorpionInterface.RoomStatus.JOINED) {
                            showError("Failed to join room: " + status);
                            shutdown();
                            return;
                        }
                        updateStatus("Joined room " + currentRoomId);
                    }
                } else if (choice == 1 && options.length > 2) { // Create Room when Join was first option
                    currentRoomId = game.createRoom(playerName);
                    updateStatus("Room created! ID: " + currentRoomId);
                    playerLabel.setText("Player: " + playerName + " (Waiting for opponent)");
                } else { // Cancel
                    shutdown();
                    return;
                }

                // Get player symbol and start game
                playerSymbol = game.getPlayerSymbol(currentRoomId, playerName);
                playerLabel.setText("Player: " + playerName + " (" + playerSymbol + ")");
                gameLoop();
            } catch (Exception e) {
                showError("Connection failed: " + e.getMessage());
                shutdown();
            }
        });
    }

    private void handleMove(int row, int col) {
        if (!buttons[row][col].getText().isEmpty() || gameEnded)
            return;

        // Optimistic UI update
        buttons[row][col].setText(playerSymbol);
        buttons[row][col].setEnabled(false);

        executor.execute(() -> {
            try {
                MorpionInterface.MoveStatus result = game.makeMove(currentRoomId, row, col, playerName);

                SwingUtilities.invokeLater(() -> {
                    if (result != MoveStatus.VALID) {
                        // Revert if invalid
                        buttons[row][col].setText("");
                        buttons[row][col].setEnabled(true);
                    }
                    updateBoard();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    buttons[row][col].setText("");
                    buttons[row][col].setEnabled(true);
                    if (!gameEnded) {
                        showError("Move failed: " + e.getMessage());
                    }
                });
            }
        });
    }

    private void gameLoop() {
        try {
            // Wait for game to start
            while (!game.isGameReady(currentRoomId)) {
                updateBoard();
                Thread.sleep(300);
            }

            // Main game loop
            while (!game.isGameOver(currentRoomId)) {
                updateBoard();
                Thread.sleep(300);
            }

            // Final update and show result
            gameEnded = true;
            updateBoard();
            showFinalResult();
        } catch (Exception e) {
            if (!gameEnded && isDisplayable()) {
                showError("Game error: " + e.getMessage());
                shutdown();
            }
        }
    }

    private void updateBoard() {
        SwingUtilities.invokeLater(() -> {
            try {
                if (!isDisplayable())
                    return;

                String boardState = game.getCurrentBoard(currentRoomId);
                if (boardState == null || boardState.isEmpty()) {
                    if (!gameEnded) {
                        showError("Game connection lost");
                        shutdown();
                    }
                    return;
                }

                // Update status
                if (game.isGameOver(currentRoomId)) {
                    String winner = game.getWinner(currentRoomId);
                    if (winner != null) {
                        if (winner.equals("DRAW")) {
                            statusLabel.setText("Game Over - It's a draw!");
                        } else {
                            statusLabel.setText("Game Over - " + winner + " wins!");
                        }
                    }
                    statusLabel.setBackground(gameOverColor);
                } else if (game.isPlayerTurn(currentRoomId, playerName)) {
                    statusLabel.setText("Your turn (Player " + playerSymbol + ")");
                    statusLabel.setBackground(yourTurnColor);
                } else {
                    statusLabel.setText("Waiting for opponent...");
                    statusLabel.setBackground(waitingColor);
                }

                // Update board
                String[] rows = boardState.split("\n");
                for (int i = 0; i < 3; i++) {
                    String rowData = rows[i * 2];
                    String[] cells = rowData.split("\\|");
                    for (int j = 0; j < 3; j++) {
                        String cell = cells[j].trim();
                        JButton btn = buttons[i][j];
                        if (!cell.equals(btn.getText())) {
                            btn.setText(cell.isEmpty() ? "" : cell);
                            btn.setEnabled(cell.isEmpty() && !game.isGameOver(currentRoomId));
                        }
                    }
                }
            } catch (Exception e) {
                if (!gameEnded && isDisplayable()) {
                    showError("Update error: " + e.getMessage());
                }
            }
        });
    }

    private void showFinalResult() {
        try {
            String winner = game.getWinner(currentRoomId);
            String message;
            String title;

            if (winner == null) {
                message = "The game has ended";
                title = "Game Over";
            } else if (winner.equals("DRAW")) {
                message = "<html><div style='text-align: center; width: 250px;'>" +
                        "<b>Game ended in a draw!</b></div></html>";
                title = "Draw!";
            } else {
                message = "<html><div style='text-align: center; width: 250px;'>" +
                        "<b>Player " + winner + " wins!</b></div></html>";
                title = "Game Over";
            }

            Object[] options = { "Play Again", "Quit" };
            int choice = JOptionPane.showOptionDialog(
                    this,
                    message,
                    title,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == JOptionPane.YES_OPTION) {
                gameEnded = false;
                game.resetGame(currentRoomId);
                resetUI();
                gameLoop();
            } else {
                shutdown();
            }
        } catch (Exception e) {
            if (isDisplayable()) {
                showError("Error showing results: " + e.getMessage());
                shutdown();
            }
        }
    }

    private void resetUI() {
        for (JButton[] row : buttons) {
            for (JButton btn : row) {
                btn.setText("");
                btn.setEnabled(true);
            }
        }
        updateStatus("Game reset - waiting for opponent...");
        statusLabel.setBackground(waitingColor);
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        if (!gameEnded && isDisplayable()) {
            JOptionPane.showMessageDialog(this,
                    "<html><div style='text-align: center;'>" + message + "</div></html>",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void shutdown() {
        try {
            if (game != null && currentRoomId != null && !gameEnded) {
                game.disconnectPlayer(currentRoomId, playerName);
            }
        } catch (Exception e) {
            System.err.println("Disconnect error: " + e.getMessage());
        } finally {
            executor.shutdown();
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new MorpionClientGUI().setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "<html><div style='text-align: center;'>" +
                                "Failed to initialize UI:<br>" + e.getMessage() + "</div></html>",
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}