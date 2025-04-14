package model;

public class GameState {
    private String[][] board;
    private String currentPlayer;
    private boolean gameOver;
    private String winner;
    private String playerX;
    private String playerO;
    private long lastActivityTime;
    private static final long TIMEOUT_MS = 30000; // 30 seconds timeout

    public GameState() {
        resetGame();
    }

    public synchronized void resetGame() {
        board = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = " ";
            }
        }
        currentPlayer = "X"; // Player X always starts
        gameOver = false;
        winner = null;
        updateActivity();
    }

    public synchronized String registerPlayer(String playerName) {
        if (playerX == null) {
            playerX = playerName;
            updateActivity();
            return "WAIT";
        } else if (playerO == null) {
            playerO = playerName;
            updateActivity();
            return "O";
        }
        return "GAME_FULL";
    }

    public synchronized void disconnectPlayer(String playerName) {
        if (playerName.equals(playerX)) {
            playerX = null;
        } else if (playerName.equals(playerO)) {
            playerO = null;
        }
        if (playerX == null && playerO == null) {
            resetGame();
        }
        updateActivity();
    }

    public synchronized boolean makeMove(int row, int col, String playerName) {
        // Validate game state
        if (gameOver)
            return false;
        if (!isGameReady())
            return false;

        // Validate player
        if (!isValidPlayer(playerName))
            return false;

        // Validate turn
        if (!isPlayerTurn(playerName))
            return false;

        // Validate move coordinates
        if (row < 0 || row > 2 || col < 0 || col > 2)
            return false;
        if (!board[row][col].equals(" "))
            return false;

        // Execute move
        board[row][col] = currentPlayer;
        checkGameOver();
        if (!gameOver) {
            currentPlayer = currentPlayer.equals("X") ? "O" : "X";
        }
        updateActivity();
        return true;
    }

    private boolean isValidPlayer(String playerName) {
        return playerName.equals(playerX) || playerName.equals(playerO);
    }

    private void checkGameOver() {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (checkLine(board[i][0], board[i][1], board[i][2])) {
                gameOver = true;
                winner = board[i][0];
                return;
            }
        }

        // Check columns
        for (int i = 0; i < 3; i++) {
            if (checkLine(board[0][i], board[1][i], board[2][i])) {
                gameOver = true;
                winner = board[0][i];
                return;
            }
        }

        // Check diagonals
        if (checkLine(board[0][0], board[1][1], board[2][2])) {
            gameOver = true;
            winner = board[0][0];
            return;
        }
        if (checkLine(board[0][2], board[1][1], board[2][0])) {
            gameOver = true;
            winner = board[0][2];
            return;
        }

        // Check for draw
        boolean isDraw = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].equals(" ")) {
                    isDraw = false;
                    break;
                }
            }
            if (!isDraw)
                break;
        }

        if (isDraw) {
            gameOver = true;
            winner = "DRAW";
        }
    }

    private boolean checkLine(String a, String b, String c) {
        return !a.equals(" ") && a.equals(b) && a.equals(c);
    }

    private void updateActivity() {
        lastActivityTime = System.currentTimeMillis();
    }

    public synchronized boolean checkTimeout() {
        return (System.currentTimeMillis() - lastActivityTime) > TIMEOUT_MS;
    }

    public synchronized boolean isPlayerTurn(String playerName) {
        return (currentPlayer.equals("X") && playerName.equals(playerX)) ||
                (currentPlayer.equals("O") && playerName.equals(playerO));
    }

    public synchronized boolean isGameReady() {
        return playerX != null && playerO != null;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    public synchronized String getWinner() {
        return winner;
    }

    public synchronized String getCurrentBoard() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append(" ").append(board[i][0]).append(" | ").append(board[i][1]).append(" | ").append(board[i][2])
                    .append(" \n");
            if (i < 2)
                sb.append("-----------\n");
        }
        return sb.toString();
    }

    public synchronized String getPlayerSymbol(String playerName) {
        if (playerName.equals(playerX))
            return "X";
        if (playerName.equals(playerO))
            return "O";
        return null;
    }

    public synchronized int getPlayerCount() {
        int count = 0;
        if (playerX != null)
            count++;
        if (playerO != null)
            count++;
        return count;
    }

    public synchronized boolean isEmpty() {
        return playerX == null && playerO == null;
    }

    /**
     * Returns the other player's name or null if not found
     */
    public synchronized String getOtherPlayer(String playerName) {
        if (playerName.equals(playerX)) {
            return playerO;
        } else if (playerName.equals(playerO)) {
            return playerX;
        }
        return null;
    }
}