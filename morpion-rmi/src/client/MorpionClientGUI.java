package client;

import shared.MorpionInterface;
import shared.MorpionInterface.RegistrationStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MorpionClientGUI extends JFrame {
    private MorpionInterface game;
    private String playerName;
    private String playerSymbol;
    private String currentRoomId;
    private final JButton[][] buttons = new JButton[3][3];
    private JLabel statusLabel;
    private JLabel roomInfoLabel;
    private JPanel mainPanel;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private volatile boolean gameRunning = false;

    // Color scheme
    private static final Color BG_COLOR = new Color(240, 240, 240);
    private static final Color BUTTON_COLOR = new Color(255, 255, 255);
    private static final Color HOVER_COLOR = new Color(230, 230, 230);
    private static final Color X_COLOR = new Color(44, 62, 80);
    private static final Color O_COLOR = new Color(231, 76, 60);
    private static final Color STATUS_COLOR = new Color(52, 152, 219);
    private static final Color ERROR_COLOR = new Color(231, 76, 60);

    public MorpionClientGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        setupGUI();
        connectToServer();
    }

    private void setupGUI() {
        setTitle("Tic-Tac-Toe (RMI)");
        setSize(500, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(BG_COLOR);

        // Status label
        statusLabel = new JLabel("Connecting to server...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(STATUS_COLOR);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Room info label
        roomInfoLabel = new JLabel("", SwingConstants.CENTER);
        roomInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Game board
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        boardPanel.setBackground(BG_COLOR);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        boardPanel.setVisible(false);

        Font buttonFont = new Font("Segoe UI", Font.BOLD, 60);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int row = i, col = j;
                buttons[i][j] = new JButton();
                buttons[i][j].setFont(buttonFont);
                buttons[i][j].setBackground(BUTTON_COLOR);
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 2));

                buttons[i][j].addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        if (buttons[row][col].getText().isEmpty()) {
                            buttons[row][col].setBackground(HOVER_COLOR);
                        }
                    }

                    public void mouseExited(MouseEvent e) {
                        if (buttons[row][col].getText().isEmpty()) {
                            buttons[row][col].setBackground(BUTTON_COLOR);
                        }
                    }
                });

                buttons[i][j].addActionListener(e -> handleMove(row, col));
                boardPanel.add(buttons[i][j]);
            }
        }

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(statusLabel, BorderLayout.CENTER);
        topPanel.add(roomInfoLabel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(boardPanel, BorderLayout.CENTER);
        add(mainPanel);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
    }

    private void connectToServer() {
        executor.submit(() -> {
            try {
                String input = JOptionPane.showInputDialog(this, "Enter your name:", "Player Registration",
                        JOptionPane.PLAIN_MESSAGE);
                if (input == null || input.trim().isEmpty()) {
                    shutdown();
                    return;
                }

                playerName = input.trim();
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                game = (MorpionInterface) registry.lookup("MorpionGame");
                SwingUtilities.invokeLater(this::showRoomSelection);
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    showError("Connection failed: " + e.getMessage());
                    shutdown();
                });
            }
        });
    }

    private void showRoomSelection() {
        JPanel roomPanel = new JPanel(new BorderLayout());
        DefaultListModel<String> roomListModel = new DefaultListModel<>();
        JList<String> roomList = new JList<>(roomListModel);

        JButton createBtn = new JButton("Create Room");
        createBtn.addActionListener(e -> createNewRoom());

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshRoomList(roomListModel));

        JButton joinBtn = new JButton("Join");
        joinBtn.addActionListener(e -> {
            String selected = roomList.getSelectedValue();
            if (selected != null && !selected.startsWith("No rooms") && !selected.startsWith("Error")) {
                joinSelectedRoom(selected);
            }
        });

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        buttonPanel.add(createBtn);
        buttonPanel.add(refreshBtn);
        buttonPanel.add(joinBtn);

        roomPanel.add(new JScrollPane(roomList), BorderLayout.CENTER);
        roomPanel.add(buttonPanel, BorderLayout.SOUTH);

        refreshRoomList(roomListModel);
        Timer refreshTimer = new Timer(2000, e -> refreshRoomList(roomListModel));
        refreshTimer.start();

        int option = JOptionPane.showOptionDialog(
                this,
                roomPanel,
                "Select Room",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                new Object[] { "Cancel" },
                null);

        if (option != JOptionPane.CLOSED_OPTION) {
            refreshTimer.stop();
        }
    }

    private void refreshRoomList(DefaultListModel<String> roomListModel) {
        executor.submit(() -> {
            try {
                List<String> rooms = game.listAvailableRooms();
                SwingUtilities.invokeLater(() -> {
                    roomListModel.clear();
                    if (rooms.isEmpty()) {
                        roomListModel.addElement("No rooms available - create one!");
                    } else {
                        // Extract just the room ID part (first token before space)
                        rooms.forEach(room -> {
                            String[] parts = room.split(" ");
                            if (parts.length > 0) {
                                roomListModel.addElement(parts[0]);
                            }
                        });
                    }
                });
            } catch (RemoteException e) {
                SwingUtilities.invokeLater(() -> {
                    roomListModel.clear();
                    roomListModel.addElement("Error fetching rooms");
                });
            }
        });
    }

    private void createNewRoom() {
        executor.submit(() -> {
            try {
                currentRoomId = game.createRoom(playerName);
                // No need to join separately - server auto-joins creator
                playerSymbol = "X"; // Creator is always X

                SwingUtilities.invokeLater(() -> {
                    updateStatus("Room created! You are Player X");
                    updateRoomInfo();
                    showGameBoard();
                    startGameLoop();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    showError("Room creation failed: " + e.getMessage());
                    System.err.println("Creation error: " + e);
                    e.printStackTrace();
                });
            }
        });
    }

    private void joinSelectedRoom(String roomInfo) {
        // Extract just the room ID (first part before space)
        String roomId = roomInfo.contains(" ") ? roomInfo.split(" ")[0] : roomInfo;

        executor.submit(() -> {
            try {
                MorpionInterface.RegistrationStatus status = game.joinRoom(roomId, playerName);

                SwingUtilities.invokeLater(() -> {
                    if (status == MorpionInterface.RegistrationStatus.PLAYER_O) {
                        currentRoomId = roomId;
                        playerSymbol = "O";
                        updateStatus("Joined as Player O");
                        showGameBoard();
                        startGameLoop();
                    } else {
                        showError("Join failed: " + status);
                        System.out.println("Join failed with status: " + status);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    showError("Join error: " + e.getMessage());
                    System.err.println("Join exception: " + e);
                    e.printStackTrace();
                });
            }
        });
    }

    private void handleMove(int row, int col) {
        if (!buttons[row][col].getText().isEmpty())
            return;

        buttons[row][col].setText(playerSymbol);
        buttons[row][col].setEnabled(false);

        executor.submit(() -> {
            try {
                MorpionInterface.MoveStatus result = game.makeMove(currentRoomId, row, col, playerName);
                SwingUtilities.invokeLater(() -> {
                    if (result != MorpionInterface.MoveStatus.VALID) {
                        buttons[row][col].setText("");
                        buttons[row][col].setEnabled(true);
                        updateStatus("Move failed: " + result);
                    }
                });
            } catch (RemoteException e) {
                SwingUtilities.invokeLater(() -> {
                    buttons[row][col].setText("");
                    buttons[row][col].setEnabled(true);
                    showError("Move error");
                });
            }
        });
    }

    private void startGameLoop() {
        if (gameRunning)
            return;
        gameRunning = true;

        executor.submit(() -> {
            try {
                while (!game.isGameOver()) {
                    updateBoard();
                    Thread.sleep(500);
                }
                showGameResult();
            } catch (Exception e) {
                showError("Game error");
            } finally {
                gameRunning = false;
            }
        });
    }

    private void updateBoard() {
        try {
            String board = game.getCurrentBoard(currentRoomId);
            if (board == null || board.isEmpty()) {
                return;
            }

            String[] rows = board.split("\n");
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < Math.min(3, rows.length); i++) {
                    String[] cells = rows[i].split("\\|");
                    for (int j = 0; j < Math.min(3, cells.length); j++) {
                        String cell = cells[j].trim();
                        buttons[i][j].setText(cell.isEmpty() ? "" : cell);
                        try {
                            buttons[i][j].setEnabled(cell.isEmpty() &&
                                    game.isPlayerTurn(currentRoomId));
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    if (game.isGameOver(currentRoomId)) {
                        String winner = game.getWinner();
                        updateStatus(winner.equals("DRAW") ? "Game ended in draw!" : winner + " wins!");
                    } else {
                        updateStatus(game.isPlayerTurn(currentRoomId) ? "Your turn (" + playerSymbol + ")"
                                : "Waiting for opponent...");
                    }
                } catch (RemoteException e) {
                    showError("Status update error");
                }
            });
        } catch (RemoteException e) {
            showError("Board update error");
        }
    }

    private void showGameResult() {
        try {
            String winner = game.getWinner();
            String message = winner.equals("DRAW") ? "It's a draw!" : winner + " wins!";

            int choice = JOptionPane.showOptionDialog(this, message, "Game Over",
                    JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, new String[] { "Play Again", "Quit" }, "Quit");

            if (choice == 0) {
                game.resetGame();
                resetUI();
                startGameLoop();
            } else {
                shutdown();
            }
        } catch (RemoteException e) {
            showError("Error getting game result");
            shutdown();
        }
    }

    private void showGameBoard() {
        ((JPanel) mainPanel.getComponent(1)).setVisible(true);
        revalidate();
        repaint();
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void updateRoomInfo() {
        roomInfoLabel.setText("Room: " + currentRoomId + " | You are: " + playerSymbol);
    }

    private void resetUI() {
        for (JButton[] row : buttons) {
            for (JButton btn : row) {
                btn.setText("");
                btn.setEnabled(false);
            }
        }
        updateStatus("Waiting for game to start...");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void shutdown() {
        executor.submit(() -> {
            try {
                if (game != null && currentRoomId != null && playerName != null) {
                    game.disconnectPlayer(playerName);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            } finally {
                executor.shutdown();
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MorpionClientGUI().setVisible(true));
    }
}