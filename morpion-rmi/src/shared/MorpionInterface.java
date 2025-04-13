package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

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
    MoveStatus makeMove(int row, int col, String playerName) throws RemoteException;

    /**
     * Gets the current board state as a formatted string
     * 
     * @return String representation of the board
     * @throws RemoteException If connection fails
     */
    String getCurrentBoard() throws RemoteException;

    /**
     * Checks if the game is over
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
     * @param playerName The player to check
     * @return true if it's the player's turn
     * @throws RemoteException If connection fails
     */
    boolean isPlayerTurn(String playerName) throws RemoteException;

    /**
     * Checks if the game has two players and is ready
     * 
     * @return true if game is ready to start
     * @throws RemoteException If connection fails
     */
    boolean isGameReady() throws RemoteException;

    /**
     * Resets the game to initial state
     * 
     * @throws RemoteException If connection fails
     */
    void resetGame() throws RemoteException;

    /**
     * Disconnects a player from the game
     * 
     * @param playerName The player to disconnect
     * @throws RemoteException If connection fails
     */
    void disconnectPlayer(String playerName) throws RemoteException;

    /**
     * Gets the symbol (X/O) assigned to a player
     * 
     * @param playerName The player to check
     * @return "X" or "O" if player exists, null otherwise
     * @throws RemoteException If connection fails
     */
    String getPlayerSymbol(String playerName) throws RemoteException;
}