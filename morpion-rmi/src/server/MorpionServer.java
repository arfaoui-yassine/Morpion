package server;

import shared.MorpionInterface;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class MorpionServer extends UnicastRemoteObject implements MorpionInterface {
    private final ConcurrentHashMap<String, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);
    private static final long ROOM_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
    private static final String DEFAULT_ROOM_ID = "default";

    public MorpionServer() throws RemoteException {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupInactiveRooms, 1, 1, TimeUnit.MINUTES);
    }

    /* Room Management Methods */
    @Override
    public synchronized String createRoom(String playerName) throws RemoteException {
        String roomId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        GameRoom newRoom = new GameRoom(roomId, playerName);
        gameRooms.put(roomId, newRoom);

        // Auto-join the creator as player X
        newRoom.registerPlayer(playerName);

        System.out.println("[SERVER] Room created: " + roomId + " by " + playerName);
        System.out.println("[SERVER] Current rooms: " + gameRooms.keySet() +
                " - Host: " + playerName +
                ", Status: " + newRoom.getStatus());
        return roomId;
    }

    @Override
    public synchronized RegistrationStatus joinRoom(String roomId, String playerName) throws RemoteException {
        GameRoom room = gameRooms.get(roomId);
        if (room == null) {
            System.out.println("[SERVER] Join failed - room not found: " + roomId);
            return RegistrationStatus.ERROR;
        }

        synchronized (room) {
            if (room.getHostPlayer().equals(playerName)) {
                System.out.println("[SERVER] Player " + playerName + " is already host of room " + roomId);
                return RegistrationStatus.ERROR;
            }

            RegistrationStatus status = room.registerPlayer(playerName);
            if (status == RegistrationStatus.PLAYER_O) {
                System.out.println("[SERVER] Player " + playerName + " joined room " + roomId);
                System.out.println("[SERVER] Room status: " + room.getStatus() +
                        ", Players: X=" + room.getHostPlayer() +
                        ", O=" + playerName);
            }
            return status;
        }
    }

    @Override
    public synchronized List<String> listAvailableRooms() throws RemoteException {
        List<String> availableRooms = new ArrayList<>();

        gameRooms.forEach((roomId, room) -> {
            synchronized (room) {
                if (!room.isFull() && !room.isInactive(ROOM_TIMEOUT)) {
                    availableRooms.add(roomId + " (Host: " + room.getHostPlayer() + ")");
                }
            }
        });

        System.out.println("[SERVER] Listing " + availableRooms.size() + " available rooms");
        return availableRooms;
    }

    /* Game Operations - Room-scoped */
    @Override
    public synchronized RoomStatus getRoomStatus(String roomId) throws RemoteException {
        GameRoom room = gameRooms.get(roomId);
        if (room == null) {
            return RoomStatus.ERROR;
        }
        return room.getRoomStatus(roomId);
    }

    public synchronized String getHostPlayer(String roomId) throws RemoteException {
        GameRoom room = gameRooms.get(roomId);
        return room != null ? room.getHostPlayer() : null;
    }

    public synchronized boolean isRoomJoinable(String roomId) throws RemoteException {
        GameRoom room = gameRooms.get(roomId);
        return room != null && room.getGuestPlayer() == null && !room.isInactive(ROOM_TIMEOUT);
    }

    @Override
    public synchronized MoveStatus makeMove(String roomId, int row, int col, String playerName) throws RemoteException {
        GameRoom room = gameRooms.get(roomId);
        if (room == null) {
            return MoveStatus.GAME_NOT_READY;
        }
        return room.makeMove(roomId, row, col, playerName);
    }

    @Override
    public synchronized String getCurrentBoard(String roomId) throws RemoteException {
        GameRoom room = gameRooms.get(roomId);
        return room != null ? room.getCurrentBoard(roomId) : "";
    }

    @Override
    public synchronized boolean isGameOver(String roomId) throws RemoteException {
        GameRoom room = gameRooms.get(roomId);
        return room != null && room.isGameOver(roomId);
    }

    public synchronized String getWinner(String roomId) throws RemoteException {
        GameRoom room = gameRooms.get(roomId);
        return room != null ? room.getWinner(roomId) : null;
    }

    public synchronized boolean isPlayerTurn(String roomId, String playerName) throws RemoteException {
        GameRoom room = gameRooms.get(roomId);
        return room != null && room.isPlayerTurn(roomId, playerName);
    }

    public synchronized boolean isGameReady(String roomId) throws RemoteException {
        GameRoom room = gameRooms.get(roomId);
        return room != null && room.isGameReady(roomId);
    }

    public synchronized void resetGame(String roomId) throws RemoteException {
        GameRoom room = gameRooms.get(roomId);
        if (room != null) {
            room.resetGame(roomId);
        }
    }

    public synchronized void disconnectPlayer(String roomId, String playerName) throws RemoteException {
        GameRoom room = gameRooms.get(roomId);
        if (room != null) {
            room.disconnectPlayer(roomId, playerName);
            if (room.getGuestPlayer() == null) {
                gameRooms.remove(roomId);
                System.out.println("[SERVER] Removed empty room: " + roomId);
            }
        }
    }

    public synchronized String getPlayerSymbol(String roomId, String playerName) throws RemoteException {
        GameRoom room = gameRooms.get(roomId);
        return room != null ? room.getPlayerSymbol(roomId, playerName) : null;
    }

    /* Game Operations - Non-room-scoped (default room) */
    @Override
    public synchronized RegistrationStatus registerPlayer(String playerName) throws RemoteException {
        return joinRoom(DEFAULT_ROOM_ID, playerName);
    }

    @Override
    public synchronized MoveStatus makeMove(int row, int col, String playerName) throws RemoteException {
        return makeMove(DEFAULT_ROOM_ID, row, col, playerName);
    }

    @Override
    public synchronized String getCurrentBoard() throws RemoteException {
        return getCurrentBoard(DEFAULT_ROOM_ID);
    }

    @Override
    public synchronized boolean isGameOver() throws RemoteException {
        return isGameOver(DEFAULT_ROOM_ID);
    }

    @Override
    public synchronized String getWinner() throws RemoteException {
        return getWinner(DEFAULT_ROOM_ID);
    }

    @Override
    public synchronized boolean isPlayerTurn(String playerName) throws RemoteException {
        return isPlayerTurn(DEFAULT_ROOM_ID, playerName);
    }

    @Override
    public synchronized boolean isGameReady() throws RemoteException {
        return isGameReady(DEFAULT_ROOM_ID);
    }

    @Override
    public synchronized void resetGame() throws RemoteException {
        resetGame(DEFAULT_ROOM_ID);
    }

    @Override
    public synchronized void disconnectPlayer(String playerName) throws RemoteException {
        disconnectPlayer(DEFAULT_ROOM_ID, playerName);
    }

    @Override
    public synchronized String getPlayerSymbol(String playerName) throws RemoteException {
        return getPlayerSymbol(DEFAULT_ROOM_ID, playerName);
    }

    /* Maintenance Methods */
    private void cleanupInactiveRooms() {
        long currentTime = System.currentTimeMillis();
        int initialCount = gameRooms.size();

        gameRooms.entrySet().removeIf(entry -> {
            GameRoom room = entry.getValue();
            if (room.isInactive(ROOM_TIMEOUT)) {
                System.out.println("[SERVER] Cleaning up inactive room: " + entry.getKey());
                return true;
            }
            return false;
        });

        if (initialCount != gameRooms.size()) {
            System.out.println("[SERVER] Room cleanup completed. Remaining rooms: " + gameRooms.size());
        }
    }

    public static void main(String[] args) {
        try {
            System.setProperty("sun.rmi.transport.tcp.responseTimeout", "5000");
            System.setProperty("sun.rmi.transport.connectionTimeout", "5000");

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("MorpionGame", new MorpionServer());
            System.out.println("[SERVER] Server ready on port 1099. Waiting for players...");
        } catch (Exception e) {
            System.err.println("[SERVER] Failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}