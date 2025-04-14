package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface MorpionInterface extends Remote {
    // Enum for move results
    enum MoveStatus {
        VALID, INVALID, NOT_YOUR_TURN, GAME_OVER, GAME_NOT_READY
    }

    // Enum for registration status
    enum RegistrationStatus {
        WAITING, PLAYER_O, GAME_FULL, ERROR
    }

    /**
     * Registers a new player to the game
     * 
     * @param playerName The name of the player to register
     * @return RegistrationStatus indicating the result
     * @throws RemoteException If connection fails
     */
    RegistrationStatus registerPlayer(String playerName) throws RemoteException;

    /**
     * Attempts to make a move on the board
     * 
     * @param row        The row index (0-2)
     * @param col        The column index (0-2)
     * @param playerName The name of the player making the move
     * @return MoveStatus indicating the result
     * @throws RemoteException If connection fails
     */
    MoveStatus makeMove(String roomId, int row, int col, String playerName) throws RemoteException;

    String getPlayerSymbol(String roomId, String playerName) throws RemoteException;

    String getCurrentBoard(String roomId) throws RemoteException;

    boolean isGameOver(String roomId) throws RemoteException;

    String getWinner(String roomId) throws RemoteException;

    boolean isPlayerTurn(String roomId, String playerName) throws RemoteException;

    boolean isGameReady(String roomId) throws RemoteException;

    void resetGame(String roomId) throws RemoteException;

    void disconnectPlayer(String roomId, String playerName) throws RemoteException;

    // Room system methods
    enum RoomStatus {
        CREATED, JOINED, FULL, NOT_FOUND, ERROR
    }

    /** Creates a new room. Returns room ID if successful. */
    String createRoom(String playerName) throws RemoteException;

    /** Joins an existing room. Returns RoomStatus. */
    RoomStatus joinRoom(String roomId, String playerName) throws RemoteException;

    /** Lists all available rooms (with available slots). */
    List<String> listAvailableRooms() throws RemoteException;
}