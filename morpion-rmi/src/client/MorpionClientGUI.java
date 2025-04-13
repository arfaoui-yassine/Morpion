package client;

import shared.MorpionInterface;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MorpionClientGUI extends JFrame {
    private MorpionInterface game;
    private String playerName;
    private String playerSymbol;
    private JButton[][] buttons = new JButton[3][3];
    private JLabel statusLabel;
    private JPanel mainPanel;
    private ExecutorService executor = Executors.newFixedThreadPool(2);

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
                // Player registration
                playerName = JOptionPane.showInputDialog(
                        this,
                        "Enter your name:",
                        "Player Registration",
                        JOptionPane.PLAIN_MESSAGE);

                if (playerName == null || playerName.trim().isEmpty()) {
                    shutdown();
                    return;
                }

                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                game = (MorpionInterface) registry.lookup("MorpionGame");

                MorpionInterface.RegistrationStatus status = game.registerPlayer(playerName);
                updateStatus(status == MorpionInterface.RegistrationStatus.WAITING ? "Waiting for opponent..."
                        : "Game started as Player "
                                + (status == MorpionInterface.RegistrationStatus.PLAYER_O ? "O" : "X"));

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

        // Immediate visual feedback
        buttons[row][col].setText(playerSymbol);
        buttons[row][col].setEnabled(false);

        executor.execute(() -> {
            try {
                MorpionInterface.MoveStatus result = game.makeMove(row, col, playerName);

                SwingUtilities.invokeLater(() -> {
                    switch (result) {
                        case VALID:
                            // Move already shown
                            break;
                        case INVALID:
                            buttons[row][col].setText("");
                            buttons[row][col].setEnabled(true);
                            updateStatus("Invalid move!");
                            break;
                        case NOT_YOUR_TURN:
                            buttons[row][col].setText("");
                            buttons[row][col].setEnabled(true);
                            updateStatus("Not your turn!");
                            break;
                        case GAME_OVER:
                            buttons[row][col].setText("");
                            buttons[row][col].setEnabled(true);
                            updateStatus("Game already over!");
                            break;
                        case GAME_NOT_READY:
                            buttons[row][col].setText("");
                            buttons[row][col].setEnabled(true);
                            updateStatus("Game not ready!");
                            break;
                    }
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
            while (!game.isGameReady()) {
                Thread.sleep(1000);
            }

            playerSymbol = game.getPlayerSymbol(playerName);
            updateStatus("Your symbol: " + playerSymbol);

            // Main game loop
            while (!game.isGameOver()) {
                updateBoard();
                Thread.sleep(300);
            }

            // Game over handling
            updateBoard();
            showGameResult();
        } catch (Exception e) {
            showError("Game error: " + e.getMessage());
            shutdown();
        }
    }

    private void updateBoard() {
        SwingUtilities.invokeLater(() -> {
            try {
                String boardState = game.getCurrentBoard();
                String[] rows = boardState.split("\n");

                for (int i = 0; i < 3; i++) {
                    String[] cells = rows[i * 2].split("\\|");
                    for (int j = 0; j < 3; j++) {
                        String cell = cells[j].trim();
                        JButton btn = buttons[i][j];

                        if (!cell.equals(btn.getText())) {
                            btn.setText(cell);
                            btn.setEnabled(cell.isEmpty() && !game.isGameOver());
                        }
                    }
                }

                if (game.isPlayerTurn(playerName)) {
                    updateStatus("Your turn (Player " + playerSymbol + ")");
                } else {
                    updateStatus("Waiting for opponent...");
                }
            } catch (Exception e) {
                showError("Update error: " + e.getMessage());
            }
        });
    }

    private void showGameResult() {
        try {
            String winner = game.getWinner();
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
                game.resetGame();
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
                game.disconnectPlayer(playerName);
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