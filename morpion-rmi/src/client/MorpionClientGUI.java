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
    private JPanel mainPanel;
    private ExecutorService executor = Executors.newFixedThreadPool(2);
    private String currentRoomId;

    // Color scheme
    private final Color bgColor = new Color(240, 240, 240);
    private final Color buttonColor = new Color(255, 255, 255);
    private final Color hoverColor = new Color(230, 230, 230);
    private final Color xColor = new Color(44, 62, 80);
    private final Color oColor = new Color(231, 76, 60);
    private final Color statusColor = new Color(52, 152, 219);
    private final Color errorColor = new Color(231, 76, 60);

    public MorpionClientGUI() {
        setupGUI();
        connectToServer();
    }

    private void setupGUI() {
        setTitle("Tic-Tac-Toe (RMI)");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main panel
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(bgColor);

        // Status label
        statusLabel = new JLabel("Connecting to server...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(statusColor);
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
                buttons[i][j].setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 2));

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

        mainPanel.add(statusLabel, BorderLayout.NORTH);
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

                playerName = JOptionPane.showInputDialog("Enter your name:");
                if (playerName == null || playerName.trim().isEmpty()) {
                    shutdown();
                    return;
                }

                // Ask: Create or Join Room?
                Object[] options = { "Create Room", "Join Room", "Cancel" };
                int choice = JOptionPane.showOptionDialog(
                        this,
                        "Create a new room or join an existing one?",
                        "Room Selection",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);

                if (choice == 0) { // Create Room
                    currentRoomId = game.createRoom(playerName);
                    updateStatus("Room created! ID: " + currentRoomId + " - Waiting for opponent...");
                } else if (choice == 1) { // Join Room
                    List<String> availableRooms = game.listAvailableRooms();
                    if (availableRooms.isEmpty()) {
                        showError("No rooms available. Create one instead.");
                        shutdown();
                        return;
                    }
                    String selectedRoom = (String) JOptionPane.showInputDialog(
                            this,
                            "Available Rooms:",
                            "Join Room",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            availableRooms.toArray(),
                            availableRooms.get(0));
                    if (selectedRoom == null) {
                        shutdown();
                        return;
                    }
                    // Extract room ID (e.g., "Room A3F9B2 (Owner: Alice)" → "A3F9B2")
                    currentRoomId = selectedRoom.split(" ")[1];
                    MorpionInterface.RoomStatus status = game.joinRoom(currentRoomId, playerName);
                    if (status != MorpionInterface.RoomStatus.JOINED) {
                        showError("Failed to join room: " + status);
                        shutdown();
                        return;
                    }
                    updateStatus("Joined room " + currentRoomId);
                } else { // Cancel
                    shutdown();
                    return;
                }

                // Proceed with game loop
                playerSymbol = game.getPlayerSymbol(currentRoomId, playerName);
                gameLoop();
            } catch (Exception e) {
                showError("Connection failed: " + e.getMessage());
                shutdown();
            }
        });
    }

    private void handleMove(int row, int col) {
        if (!buttons[row][col].getText().isEmpty())
            return;

        // Show optimistic update
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
                    updateBoard(); // Force refresh after move
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    buttons[row][col].setText("");
                    buttons[row][col].setEnabled(true);
                    showError("Move failed: " + e.getMessage());
                });
            }
        });
    }

    private void gameLoop() {
        try {
            // Wait for game to start
            while (!game.isGameReady(currentRoomId)) {
                updateBoard(); // Show waiting state
                Thread.sleep(500);
            }

            // Main game loop
            while (!game.isGameOver(currentRoomId)) {
                updateBoard();
                Thread.sleep(500); // Reduced from 300ms for better responsiveness
            }

            updateBoard(); // Final update
            showGameResult();
        } catch (Exception e) {
            showError("Game error: " + e.getMessage());
            shutdown();
        }
    }

    private void updateBoard() {
        SwingUtilities.invokeLater(() -> {
            try {
                String boardState = game.getCurrentBoard(currentRoomId);
                if (boardState == null)
                    return;
                // In updateBoard():
                if (game.isPlayerTurn(currentRoomId, playerName)) {
                    statusLabel.setText("Your turn (Player " + playerSymbol + ")");
                    statusLabel.setBackground(new Color(76, 175, 80)); // Green
                } else {
                    statusLabel.setText("Waiting for opponent...");
                    statusLabel.setBackground(statusColor); // Original blue
                }
                // Split by newline and ignore separator lines
                String[] rows = boardState.split("\n");
                for (int i = 0; i < 3; i++) {
                    // Skip separator lines (index 1, 3, etc.)
                    String rowData = rows[i * 2];
                    String[] cells = rowData.split("\\|");

                    for (int j = 0; j < 3; j++) {
                        String cell = cells[j].trim();
                        JButton btn = buttons[i][j];

                        // Only update if different
                        if (!cell.equals(btn.getText())) {
                            btn.setText(cell.isEmpty() ? "" : cell);
                            btn.setEnabled(cell.isEmpty() && !game.isGameOver(currentRoomId));
                        }
                    }
                }
            } catch (Exception e) {
                showError("Update error: " + e.getMessage());
            }
        });
    }

    private void showGameResult() {
        try {
            String winner = game.getWinner(currentRoomId);
            String message = winner.equals("DRAW")
                    ? "Game ended in a draw!"
                    : "Player " + winner + " wins!";

            int choice = JOptionPane.showOptionDialog(
                    this,
                    message,
                    "Game Over",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    new Object[] { "Play Again", "Quit" },
                    "Play Again");

            if (choice == JOptionPane.YES_OPTION) {
                game.resetGame(currentRoomId);
                resetUI();
                gameLoop();
            } else {
                shutdown();
            }
        } catch (Exception e) {
            showError(e.getMessage());
            shutdown();
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
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void shutdown() {
        try {
            if (game != null) {
                game.disconnectPlayer(currentRoomId, playerName);
            }
            executor.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new MorpionClientGUI().setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Failed to initialize UI: " + e.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}