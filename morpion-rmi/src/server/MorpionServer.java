package server;

import shared.MorpionInterface;
import shared.MorpionCallback;
import model.GameState;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

public class MorpionServer extends RemoteServer implements MorpionInterface {
    private final GameState gameState = new GameState();
    private boolean running = true;
    private final Map<String, MorpionCallback> callbacks = new ConcurrentHashMap<>();

    public MorpionServer() throws RemoteException {
        super();
        new Thread(this::cleanupTask).start();
    }

    private void cleanupTask() {
        while (running) {
            try {
                Thread.sleep(5000);
                synchronized (gameState) {
                    if (gameState.checkTimeout()) {
                        System.out.println("Resetting inactive game...");
                        String playerX = gameState.getPlayerX();
                        String playerO = gameState.getPlayerO();

                        if (playerX != null && callbacks.containsKey(playerX)) {
                            callbacks.get(playerX).opponentDisconnected();
                        }
                        if (playerO != null && callbacks.containsKey(playerO)) {
                            callbacks.get(playerO).opponentDisconnected();
                        }

                        gameState.resetGame();
                        callbacks.clear();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RemoteException e) {
                System.err.println("Error in cleanup: " + e.getMessage());
            }
        }
    }

    @Override
    public synchronized String registerPlayer(String playerName, MorpionCallback callback) throws RemoteException {
        callbacks.put(playerName, callback);
        String status = gameState.registerPlayer(playerName);

        if (gameState.isGameReady()) {
            // Notify both players that game is ready
            String playerX = gameState.getPlayerX();
            String playerO = gameState.getPlayerO();

            callbacks.get(playerX).gameReady("X");
            callbacks.get(playerO).gameReady("O");

            // Send initial board state
            String boardState = gameState.getCurrentBoard();
            callbacks.get(playerX).updateBoard(boardState);
            callbacks.get(playerO).updateBoard(boardState);
        }

        return status;
    }

    @Override
    public synchronized String makeMove(int row, int col, String playerName) throws RemoteException {
        if (!gameState.isGameReady()) {
            return "GAME_NOT_READY";
        }
        if (gameState.isGameOver()) {
            return "GAME_OVER";
        }
        if (!gameState.isPlayerTurn(playerName)) {
            return "NOT_YOUR_TURN";
        }

        if (!gameState.makeMove(row, col, playerName)) {
            return "INVALID_MOVE";
        }

        // Notify both players of board update
        String boardState = gameState.getCurrentBoard();
        String playerX = gameState.getPlayerX();
        String playerO = gameState.getPlayerO();

        if (playerX != null && callbacks.containsKey(playerX)) {
            callbacks.get(playerX).updateBoard(boardState);
        }
        if (playerO != null && callbacks.containsKey(playerO)) {
            callbacks.get(playerO).updateBoard(boardState);
        }

        if (gameState.isGameOver()) {
            String winner = gameState.getWinner();
            if (playerX != null && callbacks.containsKey(playerX)) {
                callbacks.get(playerX).gameOver(winner);
            }
            if (playerO != null && callbacks.containsKey(playerO)) {
                callbacks.get(playerO).gameOver(winner);
            }
        }

        return "VALID_MOVE";
    }

    @Override
    public synchronized String getCurrentBoard() throws RemoteException {
        return gameState.getCurrentBoard();
    }

    @Override
    public synchronized boolean isGameOver() throws RemoteException {
        return gameState.isGameOver();
    }

    @Override
    public synchronized String getWinner() throws RemoteException {
        return gameState.getWinner();
    }

    @Override
    public synchronized boolean isPlayerTurn(String playerName) throws RemoteException {
        return gameState.isPlayerTurn(playerName);
    }

    @Override
    public synchronized boolean isGameReady() throws RemoteException {
        return gameState.isGameReady();
    }

    @Override
    public synchronized void resetGame() throws RemoteException {
        gameState.resetGame();
    }

    @Override
    public synchronized void disconnectPlayer(String playerName) throws RemoteException {
        callbacks.remove(playerName);
        gameState.disconnectPlayer(playerName);

        String opponent = gameState.getOpponentName(playerName);
        if (opponent != null && callbacks.containsKey(opponent)) {
            callbacks.get(opponent).opponentDisconnected();
        }
    }

    @Override
    public synchronized String getPlayerSymbol(String playerName) throws RemoteException {
        if (gameState.getPlayerX() != null && gameState.getPlayerX().equals(playerName)) {
            return "X";
        }
        if (gameState.getPlayerO() != null && gameState.getPlayerO().equals(playerName)) {
            return "O";
        }
        return null;
    }

    @Override
    public synchronized Map<String, Integer> getPlayerStats(String playerName) throws RemoteException {
        return gameState.getPlayerStats(playerName);
    }

    @Override
    public synchronized List<String> getMatchHistory(String playerName) throws RemoteException {
        return gameState.getMatchHistory(playerName);
    }

    @Override
    public synchronized String getOpponentName(String playerName) throws RemoteException {
        return gameState.getOpponentName(playerName);
    }

    @Override
    public synchronized boolean isPlayerConnected(String playerName) throws RemoteException {
        return gameState.isPlayerConnected(playerName);
    }

    public static void main(String[] args) {
        try {
            MorpionServer server = new MorpionServer();
            MorpionInterface stub = (MorpionInterface) UnicastRemoteObject.exportObject(server, 0);

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("MorpionGame", stub);

            System.out.println("Server ready. Waiting for players...");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}