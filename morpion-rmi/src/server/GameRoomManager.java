package server;

import model.GameState;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;

public class GameRoomManager {
    private final ConcurrentHashMap<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public static class GameRoom {
        private final GameState gameState = new GameState();
        private String playerX; // Host player (always X)
        private String playerO; // Guest player (always O)
        private boolean isActive = true;
        private long lastActivityTime = System.currentTimeMillis();

        public GameRoom(String hostPlayer) {
            this.playerX = hostPlayer;
        }

        public synchronized boolean addPlayer(String playerName) {
            if (playerO == null) {
                playerO = playerName;
                updateActivity();
                return true;
            }
            return false;
        }

        public synchronized boolean isFull() {
            return playerO != null;
        }

        public synchronized boolean isAvailable() {
            return !isFull() && isActive;
        }

        public synchronized String getHostName() {
            return playerX;
        }

        public synchronized String getPlayerX() {
            return playerX;
        }

        public synchronized String getPlayerO() {
            return playerO;
        }

        public synchronized GameState getGameState() {
            updateActivity();
            return gameState;
        }

        public synchronized boolean isActive() {
            return isActive;
        }

        public synchronized void closeRoom() {
            isActive = false;
        }

        public synchronized void updateActivity() {
            lastActivityTime = System.currentTimeMillis();
        }

        public synchronized boolean isInactive(long timeoutMillis) {
            return (System.currentTimeMillis() - lastActivityTime) > timeoutMillis;
        }
    }

    public synchronized String createRoom(String playerName) {
        String roomId = generateRoomId();
        rooms.put(roomId, new GameRoom(playerName));
        System.out.println("Room created: " + roomId + " by " + playerName);
        return roomId;
    }

    public synchronized boolean joinRoom(String roomId, String playerName) {
        GameRoom room = rooms.get(roomId);
        if (room != null && room.isAvailable()) {
            boolean success = room.addPlayer(playerName);
            if (success) {
                System.out.println("Player " + playerName + " joined room " + roomId);
            }
            return success;
        }
        return false;
    }

    public synchronized List<String> getAvailableRooms() {
        List<String> availableRooms = new ArrayList<>();
        rooms.forEach((id, room) -> {
            synchronized (room) {
                if (room.isActive() && !room.isFull()) {
                    availableRooms.add(id);
                }
            }
        });
        System.out.println("DEBUG: Returning available rooms: " + availableRooms); // Debug log
        return availableRooms;
    }

    public synchronized List<String> getAllRooms() {
        return new ArrayList<>(rooms.keySet());
    }

    private String formatRoomInfo(String roomId, GameRoom room) {
        return String.format("Room %s - Host: %s %s",
                roomId,
                room.getHostName(),
                room.getPlayerO() != null ? "(Full)" : "(Waiting)");
    }

    public synchronized GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public synchronized void removeRoom(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.closeRoom();
            rooms.remove(roomId);
            System.out.println("Room removed: " + roomId);
        }
    }

    public synchronized void cleanupInactiveRooms(long timeoutMinutes) {
        long timeoutMillis = timeoutMinutes * 60 * 1000;
        rooms.entrySet().removeIf(entry -> {
            if (entry.getValue().isInactive(timeoutMillis)) {
                System.out.println("Cleaning up inactive room: " + entry.getKey());
                return true;
            }
            return false;
        });
    }

    public synchronized String getPlayerSymbol(String roomId, String playerName) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            if (playerName.equals(room.getPlayerX()))
                return "X";
            if (playerName.equals(room.getPlayerO()))
                return "O";
        }
        return null;
    }

    private String generateRoomId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}