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

    // Enum for room status
    enum RoomStatus {
        WAITING, IN_PROGRESS, COMPLETED, ERROR
    }

    /* Room Management Methods */

    /**
     * Creates a new game room
     * 
     * @param playerName Name of the player creating the room
     * @return The created room ID
     * @throws RemoteException If connection fails
     */
    String createRoom(String playerName) throws RemoteException;

    /**
     * Joins an existing game room
     * 
     * @param roomId     The room ID to join
     * @param playerName Name of the player joining
     * @return RegistrationStatus indicating the result
     * @throws RemoteException If connection fails
     */
    RegistrationStatus joinRoom(String roomId, String playerName) throws RemoteException;

    /**
     * Lists all available game rooms
     * 
     * @return List of room IDs and their statuses
     * @throws RemoteException If connection fails
     */
    List<String> listAvailableRooms() throws RemoteException;

    /**
     * Gets the status of a specific room
     * 
     * @param roomId The room ID to check
     * @return RoomStatus indicating current state
     * @throws RemoteException If connection fails
     */
    RoomStatus getRoomStatus(String roomId) throws RemoteException;

    /* Gameplay Methods */

    /**
     * Makes a move in the current room
     * 
     * @param row        The row index (0-2)
     * @param col        The column index (0-2)
     * @param playerName The player making the move
     * @return MoveStatus indicating the result
     * @throws RemoteException If connection fails
     */
    MoveStatus makeMove(int row, int col, String playerName) throws RemoteException;

    /**
     * Gets the current board state
     * 
     * @return String representation of the board
     * @throws RemoteException If connection fails
     */
    String getCurrentBoard() throws RemoteException;

    /**
     * Checks if game is over
     * 
     * @return true if game is over
     * @throws RemoteException If connection fails
     */
    boolean isGameOver() throws RemoteException;

    /**
     * Gets the winner of the game
     * 
     * @return "X", "O", "DRAW", or null if game isn't over
     * @throws RemoteException If connection fails
     */
    String getWinner() throws RemoteException;

    /**
     * Checks if it's the specified player's turn
     * 
     * @param playerName The player to verify
     * @return true if it's the player's turn
     * @throws RemoteException If connection fails
     */
    boolean isPlayerTurn(String playerName) throws RemoteException;

    /**
     * Checks if game is ready to start
     * 
     * @return true if game is ready to start
     * @throws RemoteException If connection fails
     */
    boolean isGameReady() throws RemoteException;

    /**
     * Resets the game
     * 
     * @throws RemoteException If connection fails
     */
    void resetGame() throws RemoteException;

    /**
     * Disconnects a player
     * 
     * @param playerName The player disconnecting
     * @throws RemoteException If connection fails
     */
    void disconnectPlayer(String playerName) throws RemoteException;

    /**
     * Gets a player's symbol
     * 
     * @param playerName The player to query
     * @return "X" or "O" if player exists, null otherwise
     * @throws RemoteException If connection fails
     */
    String getPlayerSymbol(String playerName) throws RemoteException;

    /**
     * Registers a player (alternative to joinRoom for single-room implementation)
     * 
     * @param playerName The player to register
     * @return RegistrationStatus indicating the result
     * @throws RemoteException If connection fails
     */
    RegistrationStatus registerPlayer(String playerName) throws RemoteException;

    /* Room-specific methods (for multi-room implementation) */

    /**
     * Makes a move in a specific room
     * 
     * @param roomId     The room ID where the move is made
     * @param row        The row index (0-2)
     * @param col        The column index (0-2)
     * @param playerName The player making the move
     * @return MoveStatus indicating the result
     * @throws RemoteException If connection fails
     */
    default MoveStatus makeMove(String roomId, int row, int col, String playerName) throws RemoteException {
        return makeMove(row, col, playerName);
    }

    /**
     * Gets the current board state of a specific room
     * 
     * @param roomId The room ID to query
     * @return String representation of the board
     * @throws RemoteException If connection fails
     */
    default String getCurrentBoard(String roomId) throws RemoteException {
        return getCurrentBoard();
    }

    /**
     * Checks if a specific room's game is over
     * 
     * @param roomId The room ID to check
     * @return true if game is over
     * @throws RemoteException If connection fails
     */
    default boolean isGameOver(String roomId) throws RemoteException {
        return isGameOver();
    }

    // ... similar default implementations for other room-specific methods ...
}