package server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Arrays;
import shared.MorpionInterface;
import shared.MorpionCallback;

public class MorpionServer implements MorpionInterface {
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> playerRoomMap = new ConcurrentHashMap<>();
    private static final long ROOM_TIMEOUT = 30 * 60 * 1000; // 30 minutes
    private static final long GAMEOVER_CLEANUP_GRACE_PERIOD = 30 * 1000; // 30 seconds

    public MorpionServer() {
        new Thread(this::cleanupTask).start();
    }

    // Cleanup task to remove inactive or finished rooms
    private void cleanupTask() {
        while (true) {
            try {
                Thread.sleep(30000); // every 30 seconds
                long currentTime = System.currentTimeMillis();
                rooms.entrySet().removeIf(entry -> {
                    boolean remove = false;
                    try {
                        GameRoom room = entry.getValue();
                        boolean isOver = room.isGameOver();
                        boolean inactiveTooLong = (currentTime - room.getLastActivity() > ROOM_TIMEOUT);

                        // Only remove if:
                        // - Room inactive for long time
                        // OR
                        // - Game is over AND grace period passed
                        remove = inactiveTooLong || (isOver && (currentTime - room.getLastActivity() > GAMEOVER_CLEANUP_GRACE_PERIOD));
                    } catch (Exception e) {
                        remove = true; // on error, remove for safety
                    }
                    if (remove) {
                        GameRoom room = entry.getValue();
                        try {
                            List<String> players = Arrays.asList(room.getPlayerX(), room.getPlayerO());
                            for (String player : players) {
                                if (player != null) {
                                    playerRoomMap.remove(player);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    return remove;
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public List<String> listAvailableRooms() throws RemoteException {
        return rooms.entrySet().stream()
            .filter(entry -> {
                try {
                    return !entry.getValue().isGameReady();
                } catch (RemoteException e) {
                    return false;
                }
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    @Override
    public synchronized String createNewRoom() throws RemoteException {
        String roomId = "room-" + UUID.randomUUID().toString().substring(0, 8);
        rooms.put(roomId, new GameRoom());
        return roomId;
    }

    @Override
    public boolean joinRoom(String roomId, String playerName, MorpionCallback callback) throws RemoteException {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            String result = room.registerPlayer(playerName, callback);
            if (result != null && (result.equals("SUCCESS") || result.equals("WAITING_FOR_OPPONENT"))) {
                playerRoomMap.put(playerName, roomId);
                return true;
            }
        }
        return false;
    }
    
    

    @Override
    public boolean isPlayerConnected(String playerName) throws RemoteException {
        return playerRoomMap.containsKey(playerName);
    }

    @Override
    public String registerPlayer(String playerName, MorpionCallback callback) throws RemoteException {
        throw new UnsupportedOperationException("Use createNewRoom or joinRoom to register players.");
    }

    private GameRoom getPlayerRoom(String playerName) throws RemoteException {
        String roomId = playerRoomMap.get(playerName);
        if (roomId == null) {
            throw new RemoteException("Player is not connected to any room.");
        }
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new RemoteException("Room not found.");
        }
        return room;
    }

    @Override
    public String makeMove(int row, int col, String playerName) throws RemoteException {
        return getPlayerRoom(playerName).makeMove(row, col, playerName);
    }

    @Override
    public String getCurrentBoard() throws RemoteException {
        throw new UnsupportedOperationException("Must specify player context.");
    }

    @Override
    public boolean isGameOver() throws RemoteException {
        throw new UnsupportedOperationException("Must specify player context.");
    }

    @Override
    public String getWinner() throws RemoteException {
        throw new UnsupportedOperationException("Must specify player context.");
    }

    @Override
    public boolean isPlayerTurn(String playerName) throws RemoteException {
        return getPlayerRoom(playerName).isPlayerTurn(playerName);
    }

    @Override
    public boolean isGameReady() throws RemoteException {
        throw new UnsupportedOperationException("Must specify player context.");
    }

    // ✅ Properly implemented resetGame(playerName)
    @Override
public void resetGame(String playerName) throws RemoteException {
    System.out.println("resetGame() called for player: " + playerName);
    GameRoom room = getPlayerRoom(playerName);
if (room != null) {
    room.resetGame();

    // Notify both players that the board has been reset
    if (room.getPlayerXCallback() != null) {
        room.getPlayerXCallback().updateBoard(room.getBoardState());
    }
    if (room.getPlayerOCallback() != null) {
        room.getPlayerOCallback().updateBoard(room.getBoardState());
    }
}
}

    // fallback, not used
    @Override
    public void resetGame() throws RemoteException {
        System.out.println("resetGame() called on MorpionServer. No direct action taken.");
    }

    @Override
    public void disconnectPlayer(String playerName) throws RemoteException {
        try {
            GameRoom room = getPlayerRoom(playerName);
            room.disconnectPlayer(playerName);
        } finally {
            playerRoomMap.remove(playerName);
        }
    }

    @Override
    public String getPlayerSymbol(String playerName) throws RemoteException {
        return getPlayerRoom(playerName).getPlayerSymbol(playerName);
    }

    @Override
    public Map<String, Integer> getPlayerStats(String playerName) throws RemoteException {
        return getPlayerRoom(playerName).getPlayerStats(playerName);
    }

    @Override
    public List<String> getMatchHistory(String playerName) throws RemoteException {
        return getPlayerRoom(playerName).getMatchHistory(playerName);
    }

    @Override
    public String getOpponentName(String playerName) throws RemoteException {
        return getPlayerRoom(playerName).getOpponentName(playerName);
    }

    public static void main(String[] args) {
        try {
            MorpionServer server = new MorpionServer();
            MorpionInterface stub = (MorpionInterface) UnicastRemoteObject.exportObject(server, 0);

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("MorpionGame", stub);

            System.out.println("Morpion RMI Server is ready.");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
