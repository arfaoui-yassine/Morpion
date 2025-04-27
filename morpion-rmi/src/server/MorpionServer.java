// server/MorpionServer.java
package server;

import shared.MorpionInterface;
import shared.MorpionCallback;
import model.GameState;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MorpionServer extends RemoteServer implements MorpionInterface {
    private final Map<String, GameState> gameStates = new ConcurrentHashMap<>();
    private final Map<String, Map<String, MorpionCallback>> gameCallbacks = new ConcurrentHashMap<>();
    private boolean running = true;

    public MorpionServer() throws RemoteException {
        super();
        startCleanupThread();
    }

    private void startCleanupThread() {
        new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(500000);
                    cleanupInactiveGames();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    private void cleanupInactiveGames() {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, GameState> entry : gameStates.entrySet()) {
            String gameId = entry.getKey();
            GameState gameState = entry.getValue();

            synchronized (gameState) {
                if (gameState.checkTimeout()) {
                    notifyPlayersOfTimeout(gameId);
                    toRemove.add(gameId);
                }
            }
        }

        toRemove.forEach(gameId -> {
            gameStates.remove(gameId);
            gameCallbacks.remove(gameId);
        });
    }

    private void notifyPlayersOfTimeout(String gameId) {
        try {
            GameState gameState = gameStates.get(gameId);
            Map<String, MorpionCallback> callbacks = gameCallbacks.get(gameId);

            if (callbacks != null) {
                String playerX = gameState.getPlayerX();
                String playerO = gameState.getPlayerO();

                if (playerX != null && callbacks.containsKey(playerX)) {
                    callbacks.get(playerX).opponentDisconnected();
                }
                if (playerO != null && callbacks.containsKey(playerO)) {
                    callbacks.get(playerO).opponentDisconnected();
                }
            }
        } catch (RemoteException e) {
            System.err.println("Error notifying players of timeout: " + e.getMessage());
        }
    }

    @Override
    public String createNewGame() throws RemoteException {
        String gameId = UUID.randomUUID().toString();
        gameStates.put(gameId, new GameState());
        gameCallbacks.put(gameId, new ConcurrentHashMap<>());
        return gameId;
    }

    @Override
    public List<String> getAvailableGames() throws RemoteException {
        List<String> availableGames = new ArrayList<>();
        for (Map.Entry<String, GameState> entry : gameStates.entrySet()) {
            if (!entry.getValue().isGameReady()) {
                availableGames.add(entry.getKey());
            }
        }
        return availableGames;
    }

    @Override
    public String registerPlayer(String gameId, String playerName, MorpionCallback callback) throws RemoteException {
        if (!gameStates.containsKey(gameId)) {
            return "GAME_NOT_FOUND";
        }

        GameState gameState = gameStates.get(gameId);
        Map<String, MorpionCallback> callbacks = gameCallbacks.get(gameId);
        callbacks.put(playerName, callback);

        String status = gameState.registerPlayer(playerName);

        if (gameState.isGameReady()) {
            notifyPlayersGameReady(gameId, gameState, callbacks);
        }

        return status;
    }

    private void notifyPlayersGameReady(String gameId, GameState gameState, Map<String, MorpionCallback> callbacks)
            throws RemoteException {
        String playerX = gameState.getPlayerX();
        String playerO = gameState.getPlayerO();

        callbacks.get(playerX).gameReady("X");
        callbacks.get(playerO).gameReady("O");

        String boardState = gameState.getCurrentBoard();
        callbacks.get(playerX).updateBoard(boardState);
        callbacks.get(playerO).updateBoard(boardState);
    }

    @Override
    public String makeMove(String gameId, int row, int col, String playerName) throws RemoteException {
        if (!gameStates.containsKey(gameId)) {
            return "GAME_NOT_FOUND";
        }

        GameState gameState = gameStates.get(gameId);
        Map<String, MorpionCallback> callbacks = gameCallbacks.get(gameId);

        synchronized (gameState) {
            if (!gameState.isGameReady())
                return "GAME_NOT_READY";
            if (gameState.isGameOver())
                return "GAME_OVER";
            if (!gameState.isPlayerTurn(playerName))
                return "NOT_YOUR_TURN";
            if (!gameState.makeMove(row, col, playerName))
                return "INVALID_MOVE";

            updatePlayersAfterMove(gameId, gameState, callbacks);
        }

        return "VALID_MOVE";
    }

    private void updatePlayersAfterMove(String gameId, GameState gameState, Map<String, MorpionCallback> callbacks)
            throws RemoteException {
        String boardState = gameState.getCurrentBoard();
        String playerX = gameState.getPlayerX();
        String playerO = gameState.getPlayerO();

        if (playerX != null)
            callbacks.get(playerX).updateBoard(boardState);
        if (playerO != null)
            callbacks.get(playerO).updateBoard(boardState);

        if (gameState.isGameOver()) {
            String winner = gameState.getWinner();
            if (playerX != null)
                callbacks.get(playerX).gameOver(winner);
            if (playerO != null)
                callbacks.get(playerO).gameOver(winner);
        }
    }

    @Override
    public String getCurrentBoard(String gameId) throws RemoteException {
        return gameStates.containsKey(gameId) ? gameStates.get(gameId).getCurrentBoard() : "";
    }

    @Override
    public boolean isGameOver(String gameId) throws RemoteException {
        return gameStates.containsKey(gameId) && gameStates.get(gameId).isGameOver();
    }

    @Override
    public String getWinner(String gameId) throws RemoteException {
        return gameStates.containsKey(gameId) ? gameStates.get(gameId).getWinner() : null;
    }

    @Override
    public boolean isPlayerTurn(String gameId, String playerName) throws RemoteException {
        return gameStates.containsKey(gameId) && gameStates.get(gameId).isPlayerTurn(playerName);
    }

    @Override
    public boolean isGameReady(String gameId) throws RemoteException {
        return gameStates.containsKey(gameId) && gameStates.get(gameId).isGameReady();
    }

    @Override
    public void resetGame(String gameId) throws RemoteException {
        if (gameStates.containsKey(gameId)) {
            gameStates.get(gameId).resetGame();
        }
    }

    @Override
    public void disconnectPlayer(String gameId, String playerName) throws RemoteException {
        if (gameStates.containsKey(gameId)) {
            GameState gameState = gameStates.get(gameId);
            Map<String, MorpionCallback> callbacks = gameCallbacks.get(gameId);

            synchronized (gameState) {
                gameState.disconnectPlayer(playerName);
                callbacks.remove(playerName);

                String opponent = gameState.getOpponentName(playerName);
                if (opponent != null && callbacks.containsKey(opponent)) {
                    callbacks.get(opponent).opponentDisconnected();
                }
            }
        }
    }

    @Override
    public String getPlayerSymbol(String gameId, String playerName) throws RemoteException {
        return gameStates.containsKey(gameId) ? gameStates.get(gameId).getPlayerSymbol(playerName) : null;
    }

    @Override
    public Map<String, Integer> getPlayerStats(String gameId, String playerName) throws RemoteException {
        return gameStates.containsKey(gameId) ? gameStates.get(gameId).getPlayerStats(playerName)
                : Collections.emptyMap();
    }

    @Override
    public List<String> getMatchHistory(String gameId, String playerName) throws RemoteException {
        return gameStates.containsKey(gameId) ? gameStates.get(gameId).getMatchHistory(playerName)
                : Collections.emptyList();
    }

    @Override
    public String getOpponentName(String gameId, String playerName) throws RemoteException {
        return gameStates.containsKey(gameId) ? gameStates.get(gameId).getOpponentName(playerName) : null;
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