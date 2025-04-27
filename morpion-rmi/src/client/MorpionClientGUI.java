package client;

import shared.MorpionInterface;
import shared.MorpionCallback;
import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.List;

public class MorpionClientGUI extends JFrame implements MorpionCallback {
    private MorpionInterface game;
    private String gameId;
    private String playerName;
    private String playerSymbol;
    private JButton[][] buttons = new JButton[3][3];
    private JLabel statusLabel;
    private JLabel gameIdLabel;
    private JLabel playerInfoLabel;
    private JComboBox<String> gameListCombo;
    private JButton createGameButton;
    private JButton joinGameButton;
    private JButton refreshButton;
    private JPanel gamePanel;
    private JPanel controlPanel;
    private boolean myTurn = false;
    private MorpionCallback callbackStub;

    // Color scheme
    private final Color BACKGROUND_COLOR = new Color(240, 240, 245);
    private final Color PANEL_COLOR = new Color(250, 250, 255);
    private final Color BUTTON_COLOR = new Color(70, 130, 180);
    private final Color BUTTON_HOVER_COLOR = new Color(100, 150, 200);
    private final Color BUTTON_TEXT_COLOR = Color.black;
    private final Color DISABLED_BUTTON_COLOR = new Color(220, 220, 220);
    private final Color GAME_BUTTON_COLOR = new Color(245, 245, 250);
    private final Color STATUS_LABEL_COLOR = new Color(50, 50, 50);

    public MorpionClientGUI() {
        setupGUI();
        connectToServer();
    }

    private void setupGUI() {
        setTitle("Morpion Battle - Multiplayer");
        setSize(650, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND_COLOR);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(BACKGROUND_COLOR);

        // Control panel
        controlPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        controlPanel.setBackground(PANEL_COLOR);
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 210), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        // Game selection
        JPanel gameSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        gameSelectionPanel.setBackground(PANEL_COLOR);

        gameListCombo = new JComboBox<>();
        gameListCombo.setPreferredSize(new Dimension(200, 30));
        gameListCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        refreshButton = createStyledButton("Refresh");
        refreshButton.addActionListener(e -> updateGameList());

        createGameButton = createStyledButton("Create Game");
        createGameButton.addActionListener(e -> createNewGame());

        joinGameButton = createStyledButton("Join Game");
        joinGameButton.addActionListener(e -> joinSelectedGame());

        gameSelectionPanel.add(new JLabel("Available Games:"));
        gameSelectionPanel.add(gameListCombo);
        gameSelectionPanel.add(refreshButton);
        gameSelectionPanel.add(createGameButton);
        gameSelectionPanel.add(joinGameButton);

        // Game info
        gameIdLabel = new JLabel("Game ID: Not connected");
        gameIdLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        playerInfoLabel = new JLabel("Player: " + (playerName != null ? playerName : ""));
        playerInfoLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        statusLabel = new JLabel("Status: Please create or join a game", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(STATUS_LABEL_COLOR);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        controlPanel.add(gameSelectionPanel);
        controlPanel.add(gameIdLabel);
        controlPanel.add(playerInfoLabel);
        controlPanel.add(statusLabel);

        // Game board
        gamePanel = new JPanel(new GridLayout(3, 3, 10, 10));
        gamePanel.setBackground(PANEL_COLOR);
        gamePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 210), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        Font buttonFont = new Font("Segoe UI", Font.BOLD, 80);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int row = i, col = j;
                buttons[i][j] = new JButton();
                buttons[i][j].setFont(buttonFont);
                buttons[i][j].setBackground(GAME_BUTTON_COLOR);
                buttons[i][j].setForeground(Color.BLACK);
                buttons[i][j].setBorder(BorderFactory.createLineBorder(new Color(180, 180, 190), 2));
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].setEnabled(false); // Initially disabled
                buttons[i][j].addActionListener(e -> {
                    if (myTurn) {
                        handleMove(row, col);
                    }
                });

                // Add hover effect
                buttons[i][j].addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        if (buttons[row][col].isEnabled()) {
                            buttons[row][col].setBackground(new Color(230, 230, 240));
                        }
                    }

                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        if (buttons[row][col].isEnabled()) {
                            buttons[row][col].setBackground(GAME_BUTTON_COLOR);
                        }
                    }
                });

                gamePanel.add(buttons[i][j]);
            }
        }

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(gamePanel, BorderLayout.CENTER);

        add(mainPanel);
        setVisible(true);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(BUTTON_COLOR);
        button.setForeground(BUTTON_TEXT_COLOR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER_COLOR);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_COLOR);
            }
        });

        return button;
    }

    private void connectToServer() {
        try {
            // Custom input dialog for player name
            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.add(new JLabel("Enter your name:"), BorderLayout.WEST);
            JTextField nameField = new JTextField(15);
            nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            panel.add(nameField, BorderLayout.CENTER);

            int result = JOptionPane.showConfirmDialog(this, panel,
                    "Player Registration", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                playerName = nameField.getText().trim();
            }

            if (playerName == null || playerName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name cannot be empty. Exiting...",
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }

            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            game = (MorpionInterface) registry.lookup("MorpionGame");

            playerInfoLabel.setText("Player: " + playerName);
            updateGameList();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to server: " + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void updateGameList() {
        try {
            List<String> games = game.getAvailableGames();
            gameListCombo.removeAllItems();
            games.forEach(gameListCombo::addItem);
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this, "Error updating game list: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createNewGame() {
        try {
            gameId = game.createNewGame();
            gameIdLabel.setText("Game ID: " + gameId);
            joinGame();
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this, "Error creating game: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void joinSelectedGame() {
        gameId = (String) gameListCombo.getSelectedItem();
        if (gameId == null) {
            JOptionPane.showMessageDialog(this, "Please select a game to join",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        gameIdLabel.setText("Game ID: " + gameId);
        joinGame();
    }

    private void joinGame() {
        try {
            // Unexport the previous callback if it exists
            if (callbackStub != null) {
                try {
                    UnicastRemoteObject.unexportObject(this, true);
                } catch (Exception e) {
                    System.out.println("Could not unexport previous callback: " + e.getMessage());
                }
            }

            // Export the new callback
            callbackStub = (MorpionCallback) UnicastRemoteObject.exportObject(this, 0);
            String status = game.registerPlayer(gameId, playerName, callbackStub);
            updateStatus(status.equals("WAIT") ? "Waiting for opponent..." : "Game started!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to join game: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void handleMove(int row, int col) {
        try {
            String result = game.makeMove(gameId, row, col, playerName);
            if (!result.equals("VALID_MOVE")) {
                JOptionPane.showMessageDialog(this, result, "Move Error", JOptionPane.WARNING_MESSAGE);
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this, "Error making move: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void updateBoard(String boardState) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            String[] rows = boardState.split("\n");
            for (int i = 0; i < 3; i++) {
                String[] cells = rows[i * 2].split("\\|");
                for (int j = 0; j < 3; j++) {
                    String cell = cells[j].trim();
                    buttons[i][j].setText(cell.equals(" ") ? "" : cell);
                    if (!cell.equals(" ")) {
                        buttons[i][j]
                                .setForeground(cell.equals("X") ? new Color(220, 50, 50) : new Color(50, 120, 200));
                    }
                }
            }

            try {
                myTurn = game.isPlayerTurn(gameId, playerName);
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        buttons[i][j].setEnabled(myTurn && buttons[i][j].getText().isEmpty());
                        buttons[i][j].setBackground(
                                buttons[i][j].isEnabled() ? GAME_BUTTON_COLOR : new Color(240, 240, 245));
                    }
                }
                statusLabel.setText(myTurn ? "YOUR TURN - Player " + playerSymbol : "Waiting for opponent's move...");
                statusLabel.setForeground(myTurn ? new Color(0, 120, 0) : STATUS_LABEL_COLOR);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void gameReady(String playerSymbol) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            this.playerSymbol = playerSymbol;
            updateStatus("Game started! You are Player " + playerSymbol);
            resetBoard();
        });
    }

    @Override
    public void gameOver(String winner) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            String message = "DRAW".equals(winner) ? "Game ended in a draw!"
                    : winner.equals(playerName) ? "You won!" : "You lost!";

            // Custom game over dialog
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel messageLabel = new JLabel(message, SwingConstants.CENTER);
            messageLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            panel.add(messageLabel, BorderLayout.CENTER);

            JLabel questionLabel = new JLabel("Would you like to play again?", SwingConstants.CENTER);
            questionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            panel.add(questionLabel, BorderLayout.SOUTH);

            int choice = JOptionPane.showOptionDialog(this,
                    panel,
                    "Game Over",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    new Object[] { "Play Again", "Quit" },
                    "Play Again");

            try {
                if (choice == JOptionPane.YES_OPTION) {
                    // Unregister before resetting
                    game.disconnectPlayer(gameId, playerName);

                    // Reset the game
                    game.resetGame(gameId);

                    // Rejoin the game
                    String newGameId = game.createNewGame();
                    gameId = newGameId;
                    gameIdLabel.setText("Game ID: " + gameId);
                    joinGame();

                    resetBoard();
                    playerSymbol = null;
                    updateStatus("Waiting for opponent...");
                } else {
                    game.disconnectPlayer(gameId, playerName);
                    System.exit(0);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error resetting game: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @Override
    public void opponentDisconnected() throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "Your opponent has disconnected. The game will be reset.",
                    "Opponent Disconnected",
                    JOptionPane.INFORMATION_MESSAGE);
            try {
                // Unregister before resetting
                game.disconnectPlayer(gameId, playerName);

                // Reset the game
                game.resetGame(gameId);

                // Create a new game and join
                String newGameId = game.createNewGame();
                gameId = newGameId;
                gameIdLabel.setText("Game ID: " + gameId);
                joinGame();

                resetBoard();
                playerSymbol = null;
                updateStatus("Waiting for new opponent...");
            } catch (RemoteException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error resetting game: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void updateStatus(String message) {
        statusLabel.setText("Status: " + message);
    }

    private void resetBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(false);
                buttons[i][j].setBackground(GAME_BUTTON_COLOR);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new MorpionClientGUI();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}