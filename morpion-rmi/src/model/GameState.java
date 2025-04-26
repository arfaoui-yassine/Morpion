package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameState {
    private String[][] board;
    private String currentPlayer;
    private boolean gameOver;
    private String winner;
    private String playerX;
    private String playerO;
    private long lastActivityTime;
    private static final long TIMEOUT_MS = 30000;

    private Map<String, Integer> playerWins = new HashMap<>();
    private Map<String, Integer> playerLosses = new HashMap<>();
    private Map<String, Integer> playerDraws = new HashMap<>();
    private List<String> matchHistory = new ArrayList<>();
    private Random random = new Random();

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
        currentPlayer = random.nextBoolean() ? "X" : "O";
        gameOver = false;
        winner = null;
        updateActivity();
    }

    public synchronized String getPlayerX() {
        return playerX;
    }

    public synchronized String getPlayerO() {
        return playerO;
    }

    public synchronized String registerPlayer(String playerName) {
        if (playerX == null) {
            playerX = playerName;
            updateActivity();
            return "WAIT";
        } else if (playerO == null) {
            playerO = playerName;
            updateActivity();
            playerWins.putIfAbsent(playerX, 0);
            playerWins.putIfAbsent(playerO, 0);
            playerLosses.putIfAbsent(playerX, 0);
            playerLosses.putIfAbsent(playerO, 0);
            playerDraws.putIfAbsent(playerX, 0);
            playerDraws.putIfAbsent(playerO, 0);
            return currentPlayer.equals("X") ? "X" : "O";
        }
        return "GAME_FULL";
    }

    public synchronized void disconnectPlayer(String playerName) {
        if (playerName.equals(playerX)) {
            playerX = null;
        } else if (playerName.equals(playerO)) {
            playerO = null;
        }
        if (playerX == null || playerO == null) {
            resetGame();
        }
        updateActivity();
    }

    public synchronized boolean makeMove(int row, int col, String playerName) {
        if (gameOver || !isGameReady())
            return false;
        if (!isValidPlayer(playerName) || !isPlayerTurn(playerName))
            return false;
        if (row < 0 || row > 2 || col < 0 || col > 2 || !board[row][col].equals(" "))
            return false;

        board[row][col] = currentPlayer;
        checkGameOver();

        if (!gameOver) {
            currentPlayer = currentPlayer.equals("X") ? "O" : "X";
        } else {
            updatePlayerStats();
            addToMatchHistory();
        }

        updateActivity();
        return true;
    }

    private void updatePlayerStats() {
        if ("DRAW".equals(winner)) {
            playerDraws.put(playerX, playerDraws.get(playerX) + 1);
            playerDraws.put(playerO, playerDraws.get(playerO) + 1);
        } else if (playerX.equals(winner)) {
            playerWins.put(playerX, playerWins.get(playerX) + 1);
            playerLosses.put(playerO, playerLosses.get(playerO) + 1);
        } else {
            playerWins.put(playerO, playerWins.get(playerO) + 1);
            playerLosses.put(playerX, playerLosses.get(playerX) + 1);
        }
    }

    private void addToMatchHistory() {
        String result;
        if ("DRAW".equals(winner)) {
            result = "Draw between " + playerX + " and " + playerO;
        } else {
            result = winner + " won against " + (winner.equals(playerX) ? playerO : playerX);
        }
        matchHistory.add(result);
    }

    private boolean isValidPlayer(String playerName) {
        return playerName.equals(playerX) || playerName.equals(playerO);
    }

    private void checkGameOver() {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (checkLine(board[i][0], board[i][1], board[i][2])) {
                gameOver = true;
                winner = getPlayerNameBySymbol(board[i][0]);
                return;
            }
        }

        // Check columns
        for (int i = 0; i < 3; i++) {
            if (checkLine(board[0][i], board[1][i], board[2][i])) {
                gameOver = true;
                winner = getPlayerNameBySymbol(board[0][i]);
                return;
            }
        }

        // Check diagonals
        if (checkLine(board[0][0], board[1][1], board[2][2])) {
            gameOver = true;
            winner = getPlayerNameBySymbol(board[0][0]);
            return;
        }
        if (checkLine(board[0][2], board[1][1], board[2][0])) {
            gameOver = true;
            winner = getPlayerNameBySymbol(board[0][2]);
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

    private String getPlayerNameBySymbol(String symbol) {
        if ("X".equals(symbol)) {
            return playerX;
        } else if ("O".equals(symbol)) {
            return playerO;
        }
        return null;
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

    public synchronized Map<String, Integer> getPlayerStats(String playerName) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("wins", playerWins.getOrDefault(playerName, 0));
        stats.put("losses", playerLosses.getOrDefault(playerName, 0));
        stats.put("draws", playerDraws.getOrDefault(playerName, 0));
        return stats;
    }

    public synchronized List<String> getMatchHistory(String playerName) {
        List<String> playerHistory = new ArrayList<>();
        for (String match : matchHistory) {
            if (match.contains(playerName)) {
                playerHistory.add(match);
            }
        }
        return playerHistory;
    }

    public synchronized String getOpponentName(String playerName) {
        if (playerName.equals(playerX))
            return playerO;
        if (playerName.equals(playerO))
            return playerX;
        return null;
    }

    public synchronized boolean isPlayerConnected(String playerName) {
        return playerName.equals(playerX) || playerName.equals(playerO);
    }
}