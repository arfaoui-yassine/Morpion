package server;

import shared.MorpionInterface;
import model.GameState;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Objects;

public class GameRoom implements MorpionInterface {
    private final GameState gameState;
    private final String roomId;
    private RoomStatus status;
    private long lastActivityTime;
    private final String hostPlayer;
    private String guestPlayer;

    public GameRoom(String roomId, String hostPlayer) {
        this.roomId = Objects.requireNonNull(roomId);
        this.hostPlayer = Objects.requireNonNull(hostPlayer);
        this.gameState = new GameState();
        this.status = RoomStatus.WAITING;
        this.lastActivityTime = System.currentTimeMillis();
        this.gameState.registerPlayer(hostPlayer);
    }

    // Add this method to get room status
    public synchronized RoomStatus getStatus() {
        return status;
    }

    @Override
    public synchronized RegistrationStatus registerPlayer(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return RegistrationStatus.ERROR;
        }

        // Host is already registered during creation
        if (playerName.equals(hostPlayer)) {
            return RegistrationStatus.ERROR;
        }

        // If we already have a guest player
        if (guestPlayer != null) {
            return RegistrationStatus.GAME_FULL;
        }

        // Register guest player
        guestPlayer = playerName;
        status = RoomStatus.IN_PROGRESS;
        updateActivity();

        System.out.println("[ROOM] Registered " + playerName + " as O in room " + roomId);
        return RegistrationStatus.PLAYER_O;
    }

    public synchronized boolean isFull() {
        return guestPlayer != null;
    }

    @Override
    public synchronized MoveStatus makeMove(String roomId, int row, int col, String playerName) throws RemoteException {
        if (!this.roomId.equals(roomId)) {
            return MoveStatus.GAME_NOT_READY;
        }

        if (status != RoomStatus.IN_PROGRESS) {
            return MoveStatus.GAME_NOT_READY;
        }

        if (gameState.isGameOver()) {
            return MoveStatus.GAME_OVER;
        }

        if (!gameState.isPlayerTurn(playerName)) {
            return MoveStatus.NOT_YOUR_TURN;
        }

        boolean moveSuccess = gameState.makeMove(row, col, playerName);
        updateActivity();

        if (moveSuccess) {
            if (gameState.isGameOver()) {
                status = RoomStatus.COMPLETED;
            }
            return MoveStatus.VALID;
        }
        return MoveStatus.INVALID;
    }

    @Override
    public synchronized String getCurrentBoard(String roomId) throws RemoteException {
        if (!this.roomId.equals(roomId)) {
            return "";
        }
        updateActivity();
        return gameState.getCurrentBoard();
    }

    @Override
    public synchronized boolean isGameOver(String roomId) throws RemoteException {
        if (!this.roomId.equals(roomId)) {
            return false;
        }
        updateActivity();
        return gameState.isGameOver();
    }

    public synchronized String getWinner(String roomId) throws RemoteException {
        if (!this.roomId.equals(roomId)) {
            return null;
        }
        updateActivity();
        return gameState.getWinner();
    }

    public synchronized boolean isPlayerTurn(String roomId, String playerName) throws RemoteException {
        if (!this.roomId.equals(roomId)) {
            return false;
        }
        updateActivity();
        return gameState.isPlayerTurn(playerName);
    }

    public synchronized boolean isGameReady(String roomId) throws RemoteException {
        if (!this.roomId.equals(roomId)) {
            return false;
        }
        updateActivity();
        return guestPlayer != null;
    }

    public synchronized void resetGame(String roomId) throws RemoteException {
        if (this.roomId.equals(roomId)) {
            gameState.resetGame();
            status = RoomStatus.IN_PROGRESS;
            updateActivity();
        }
    }

    public synchronized void disconnectPlayer(String roomId, String playerName) throws RemoteException {
        if (this.roomId.equals(roomId)) {
            if (playerName.equals(hostPlayer)) {
                gameState.disconnectPlayer(hostPlayer);
                if (guestPlayer != null) {
                    gameState.disconnectPlayer(guestPlayer);
                }
            } else if (playerName.equals(guestPlayer)) {
                gameState.disconnectPlayer(guestPlayer);
                guestPlayer = null;
            }
            updateActivity();
            status = RoomStatus.WAITING;
        }
    }

    public synchronized String getPlayerSymbol(String roomId, String playerName) throws RemoteException {
        if (!this.roomId.equals(roomId)) {
            return null;
        }
        updateActivity();
        if (playerName.equals(hostPlayer)) {
            return "X";
        } else if (playerName.equals(guestPlayer)) {
            return "O";
        }
        return null;
    }

    @Override
    public String createRoom(String playerName) throws RemoteException {
        throw new UnsupportedOperationException("Use GameRoomManager to create rooms");
    }

    @Override
    public RegistrationStatus joinRoom(String roomId, String playerName) throws RemoteException {
        return registerPlayer(playerName);
    }

    @Override
    public List<String> listAvailableRooms() throws RemoteException {
        throw new UnsupportedOperationException("Use GameRoomManager to list rooms");
    }

    @Override
    public synchronized RoomStatus getRoomStatus(String roomId) throws RemoteException {
        if (this.roomId.equals(roomId)) {
            return status;
        }
        return RoomStatus.ERROR;
    }

    // Non-interface methods for room management
    public synchronized boolean isInactive(long timeoutMillis) {
        return (System.currentTimeMillis() - lastActivityTime) > timeoutMillis;
    }

    public synchronized boolean isEmpty() {
        return guestPlayer == null;
    }

    public synchronized String getHostPlayer() {
        return hostPlayer;
    }

    public synchronized String getGuestPlayer() {
        return guestPlayer;
    }

    private synchronized void updateActivity() {
        lastActivityTime = System.currentTimeMillis();
    }

    // Default implementations of non-room-scoped methods
    @Override
    public MoveStatus makeMove(int row, int col, String playerName) throws RemoteException {
        return makeMove(roomId, row, col, playerName);
    }

    @Override
    public String getCurrentBoard() throws RemoteException {
        return getCurrentBoard(roomId);
    }

    @Override
    public boolean isGameOver() throws RemoteException {
        return isGameOver(roomId);
    }

    @Override
    public String getWinner() throws RemoteException {
        return getWinner(roomId);
    }

    @Override
    public boolean isPlayerTurn(String playerName) throws RemoteException {
        return isPlayerTurn(roomId, playerName);
    }

    @Override
    public boolean isGameReady() throws RemoteException {
        return isGameReady(roomId);
    }

    @Override
    public void resetGame() throws RemoteException {
        resetGame(roomId);
    }

    @Override
    public void disconnectPlayer(String playerName) throws RemoteException {
        disconnectPlayer(roomId, playerName);
    }

    @Override
    public String getPlayerSymbol(String playerName) throws RemoteException {
        return getPlayerSymbol(roomId, playerName);
    }
}