package server;

import shared.MorpionInterface;
import shared.MorpionCallback;
import model.GameState;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class GameRoom extends RemoteServer{
    private final GameState gameState = new GameState();
    private final Map<String, MorpionCallback> callbacks = new ConcurrentHashMap<>();
    private long lastActivityTime = System.currentTimeMillis();

    public GameRoom() throws RemoteException {
        super();
    }

    public long getLastActivity() {
        return lastActivityTime;
    }

    private void updateActivity() {
        lastActivityTime = System.currentTimeMillis();
    }

    
    public synchronized String registerPlayer(String playerName, MorpionCallback callback) throws RemoteException {
        updateActivity();
        callbacks.put(playerName, callback);
        String status = gameState.registerPlayer(playerName);

        if (gameState.isGameReady()) {
            String playerX = gameState.getPlayerX();
            String playerO = gameState.getPlayerO();
            callbacks.get(playerX).gameReady("X");
            callbacks.get(playerO).gameReady("O");
            String boardState = gameState.getCurrentBoard();
            callbacks.get(playerX).updateBoard(boardState);
            callbacks.get(playerO).updateBoard(boardState);
        }
        return status;
    }
    
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

    
public List<String> listAvailableRooms() throws RemoteException {
    throw new UnsupportedOperationException("Not supported in GameRoom.");
}


public String createNewRoom() throws RemoteException {
    throw new UnsupportedOperationException("Not supported in GameRoom.");
}


public boolean joinRoom(String roomId, String playerName, MorpionCallback callback) throws RemoteException {
    throw new UnsupportedOperationException("Not supported in GameRoom.");
}

    
    public synchronized String getCurrentBoard() throws RemoteException {
        return gameState.getCurrentBoard();
    }

    
    public synchronized boolean isGameOver() throws RemoteException {
        return gameState.isGameOver();
    }

    
    public synchronized String getWinner() throws RemoteException {
        return gameState.getWinner();
    }

    
    public synchronized boolean isPlayerTurn(String playerName) throws RemoteException {
        return gameState.isPlayerTurn(playerName);
    }

    
    public synchronized boolean isGameReady() throws RemoteException {
        return gameState.isGameReady();
    }

    
public synchronized void resetGame() throws RemoteException {
    gameState.resetGame();
}

    
public synchronized void disconnectPlayer(String playerName) throws RemoteException {
    try {
        UnicastRemoteObject.unexportObject(callbacks.get(playerName), true);
    } catch (Exception e) {
        // Handle exception
    }
    callbacks.remove(playerName);
    gameState.disconnectPlayer(playerName);
}

    
    public synchronized String getPlayerSymbol(String playerName) throws RemoteException {
        if (gameState.getPlayerX() != null && gameState.getPlayerX().equals(playerName)) {
            return "X";
        }
        if (gameState.getPlayerO() != null && gameState.getPlayerO().equals(playerName)) {
            return "O";
        }
        return null;
    }

    
    public synchronized Map<String, Integer> getPlayerStats(String playerName) throws RemoteException {
        return gameState.getPlayerStats(playerName);
    }

    
    public synchronized List<String> getMatchHistory(String playerName) throws RemoteException {
        return gameState.getMatchHistory(playerName);
    }

    
    public synchronized String getOpponentName(String playerName) throws RemoteException {
        return gameState.getOpponentName(playerName);
    }

    
    public synchronized boolean isPlayerConnected(String playerName) throws RemoteException {
        return gameState.isPlayerConnected(playerName);
    }

    public synchronized String getPlayerX() {
        return gameState.getPlayerX();
    }
    
    public synchronized String getPlayerO() {
        return gameState.getPlayerO();
    }

    // Return the callback of player X
public synchronized MorpionCallback getPlayerXCallback() {
    String playerX = gameState.getPlayerX();
    if (playerX != null) {
        return callbacks.get(playerX);
    }
    return null;
}

// Return the callback of player O
public synchronized MorpionCallback getPlayerOCallback() {
    String playerO = gameState.getPlayerO();
    if (playerO != null) {
        return callbacks.get(playerO);
    }
    return null;
}

// Return the current board state
public synchronized String getBoardState() {
    return gameState.getCurrentBoard();
}

}