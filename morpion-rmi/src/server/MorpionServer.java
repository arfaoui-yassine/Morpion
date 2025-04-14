package server;

import shared.MorpionInterface;
import model.GameState;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MorpionServer extends UnicastRemoteObject implements MorpionInterface {
    private final Map<String, GameState> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> roomOwners = new ConcurrentHashMap<>(); // roomId → ownerName
    private final GameState gameState = new GameState();
    private boolean running = true;

    public MorpionServer() throws RemoteException {
        new Thread(this::cleanupTask).start();
    }

    private void cleanupTask() {
        while (running) {
            try {
                Thread.sleep(30000);
                new ArrayList<>(rooms.keySet()).forEach(roomId -> { // Thread-safe iteration
                    synchronized (this) { // Prevent concurrent modification
                        GameState room = rooms.get(roomId);
                        if (room != null && room.checkTimeout()) {
                            rooms.remove(roomId);
                            roomOwners.remove(roomId); // Keep in sync
                        }
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public synchronized MorpionInterface.RegistrationStatus registerPlayer(String playerName) throws RemoteException {
        String result = gameState.registerPlayer(playerName);
        return switch (result) {
            case "WAIT" -> RegistrationStatus.WAITING;
            case "O" -> RegistrationStatus.PLAYER_O;
            case "GAME_FULL" -> RegistrationStatus.GAME_FULL;
            default -> RegistrationStatus.ERROR;
        };
    }

    @Override
    public synchronized String createRoom(String playerName) throws RemoteException {
        String roomId = UUID.randomUUID().toString().substring(0, 6);
        rooms.computeIfAbsent(roomId, id -> {
            GameState newGame = new GameState();
            newGame.registerPlayer(playerName);
            roomOwners.put(id, playerName);
            return newGame;
        });
        return roomId;
    }

    @Override
    public synchronized RoomStatus joinRoom(String roomId, String playerName) throws RemoteException {
        GameState room = rooms.get(roomId);
        if (room == null)
            return RoomStatus.NOT_FOUND;
        if (room.isGameReady())
            return RoomStatus.FULL;

        String result = room.registerPlayer(playerName);
        return switch (result) {
            case "O" -> RoomStatus.JOINED;
            case "WAIT" -> RoomStatus.JOINED; // Shouldn't happen since we check isGameReady()
            case "GAME_FULL" -> RoomStatus.FULL;
            default -> RoomStatus.ERROR;
        };
    }

    @Override
    public synchronized List<String> listAvailableRooms() throws RemoteException {
        return rooms.entrySet().stream()
                .filter(entry -> !entry.getValue().isGameReady())
                .map(entry -> "Room " + entry.getKey() + " (Owner: " + roomOwners.get(entry.getKey()) + ")")
                .collect(Collectors.toList());
    }

    @Override
    public synchronized MorpionInterface.MoveStatus makeMove(String roomId, int row, int col, String playerName)
            throws RemoteException {
        GameState room = rooms.get(roomId);
        if (room == null)
            return MoveStatus.GAME_NOT_READY;
        if (!room.isGameReady())
            return MoveStatus.GAME_NOT_READY;
        if (room.isGameOver())
            return MoveStatus.GAME_OVER;
        if (!room.isPlayerTurn(playerName))
            return MoveStatus.NOT_YOUR_TURN;

        return room.makeMove(row, col, playerName) ? MoveStatus.VALID : MoveStatus.INVALID;
    }

    @Override
    public synchronized String getCurrentBoard(String roomId) throws RemoteException {
        GameState room = rooms.get(roomId);
        return (room != null) ? room.getCurrentBoard() : "";
    }

    @Override
    public synchronized boolean isGameOver(String roomId) throws RemoteException {
        GameState room = rooms.get(roomId);
        return (room != null) && room.isGameOver();
    }

    @Override
    public synchronized String getWinner(String roomId) throws RemoteException {
        return gameState.getWinner();
    }

    @Override
    public synchronized boolean isPlayerTurn(String roomId, String playerName) throws RemoteException {
        return gameState.isPlayerTurn(playerName);
    }

    @Override
    public synchronized boolean isGameReady(String roomId) throws RemoteException {
        return gameState.isGameReady();
    }

    @Override
    public synchronized void resetGame(String roomId) throws RemoteException {
        gameState.resetGame();
    }

    @Override
    public synchronized void disconnectPlayer(String roomId, String playerName) throws RemoteException {
        GameState room = rooms.get(roomId);
        if (room != null) {
            room.disconnectPlayer(playerName);

            // Safe removal only if both maps contain the room
            if (room.getPlayerCount() == 0) {
                synchronized (this) {
                    if (rooms.containsKey(roomId)) { // Double-check
                        rooms.remove(roomId);
                        roomOwners.remove(roomId); // Now safe
                    }
                }
            }
            // Handle ownership transfer if needed
            else if (playerName.equals(roomOwners.get(roomId))) {
                String newOwner = room.getOtherPlayer(playerName);
                if (newOwner != null) {
                    roomOwners.put(roomId, newOwner);
                }
            }
        }
    }

    // Fixed getPlayerSymbol():
    @Override
    public synchronized String getPlayerSymbol(String roomId, String playerName) {
        GameState room = rooms.get(roomId);
        return (room != null) ? room.getPlayerSymbol(playerName) : null;
    }

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("MorpionGame", new MorpionServer());
            System.out.println("Server ready. Waiting for players...");

            // Add shutdown hook for clean termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
            }));
        } catch (Exception e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}