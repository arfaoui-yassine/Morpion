package client;

import shared.MorpionInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MorpionClientGUI extends JFrame {
    private MorpionInterface game;
    private String playerName;
    private String playerSymbol;
    private JButton[][] buttons = new JButton[3][3];
    private JLabel statusLabel = new JLabel("Connecting...");
    private boolean myTurn = false;

    public MorpionClientGUI() {
        setupGUI();
        connectToServer();
    }

    private void setupGUI() {
        setTitle("Morpion Game (RMI)");
        setSize(300, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel boardPanel = new JPanel(new GridLayout(3, 3));
        Font font = new Font("Arial", Font.BOLD, 40);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int row = i, col = j;
                buttons[i][j] = new JButton(" ");
                buttons[i][j].setFont(font);
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].addActionListener((ActionEvent e) -> handleMove(row, col));
                boardPanel.add(buttons[i][j]);
            }
        }

        add(statusLabel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
    }

    private void connectToServer() {
        try {
            String name = JOptionPane.showInputDialog(this, "Enter your name:");
            if (name == null || name.isEmpty())
                return;

            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            game = (MorpionInterface) registry.lookup("MorpionGame");

            this.playerName = name;
            String status = game.registerPlayer(name);

            if (status.equals("WAIT")) {
                statusLabel.setText("Waiting for opponent...");
            } else {
                statusLabel.setText("You are Player O");
            }

            new Thread(this::gameLoop).start();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error connecting to server.");
            System.exit(1);
        }
    }

    private void handleMove(int row, int col) {
        if (!myTurn)
            return;
        try {
            String result = game.makeMove(row, col, playerName);
            if (!result.equals("VALID_MOVE")) {
                JOptionPane.showMessageDialog(this, "Invalid move!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void gameLoop() {
        try {
            while (!game.isGameReady()) {
                Thread.sleep(1000);
            }

            playerSymbol = game.getPlayerSymbol(playerName);
            statusLabel.setText("Game started! You are " + playerSymbol);

            while (!game.isGameOver()) {
                myTurn = game.isPlayerTurn(playerName);
                if (myTurn) {
                    statusLabel.setText("Your turn (" + playerSymbol + ")");
                } else {
                    statusLabel.setText("Waiting for opponent...");
                }

                String[][] board = getBoardMatrix(game.getCurrentBoard());
                updateBoardButtons(board);

                Thread.sleep(1000);
            }

            updateBoardButtons(getBoardMatrix(game.getCurrentBoard()));
            String winner = game.getWinner();
            if (winner.equals("Draw")) {
                JOptionPane.showMessageDialog(this, "It's a draw!");
            } else {
                JOptionPane.showMessageDialog(this, "Player " + winner + " wins!");
            }

            // Ask to replay
            int choice = JOptionPane.showConfirmDialog(this, "Play again?", "Game Over", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                game.resetGame();
                gameLoop();
            } else {
                game.disconnectPlayer(playerName);
                System.exit(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateBoardButtons(String[][] board) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    buttons[i][j].setText(board[i][j]);
                }
            }
        });
    }

    private String[][] getBoardMatrix(String boardString) {
        String[] lines = boardString.split("\n");
        String[][] matrix = new String[3][3];
        for (int i = 0; i < 3; i++) {
            String[] cells = lines[i * 2].split("\\|");
            for (int j = 0; j < 3; j++) {
                matrix[i][j] = cells[j].trim();
            }
        }
        return matrix;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MorpionClientGUI gui = new MorpionClientGUI();
            gui.setVisible(true);
        });
    }
}
