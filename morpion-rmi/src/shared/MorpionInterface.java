// shared/MorpionInterface.java
package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface MorpionInterface extends Remote {
    String createNewGame() throws RemoteException;

    List<String> getAvailableGames() throws RemoteException;

    String registerPlayer(String gameId, String playerName, MorpionCallback callback) throws RemoteException;

    String makeMove(String gameId, int row, int col, String playerName) throws RemoteException;

    String getCurrentBoard(String gameId) throws RemoteException;

    boolean isGameOver(String gameId) throws RemoteException;

    String getWinner(String gameId) throws RemoteException;

    boolean isPlayerTurn(String gameId, String playerName) throws RemoteException;

    boolean isGameReady(String gameId) throws RemoteException;

    void resetGame(String gameId) throws RemoteException;

    void disconnectPlayer(String gameId, String playerName) throws RemoteException;

    String getPlayerSymbol(String gameId, String playerName) throws RemoteException;

    Map<String, Integer> getPlayerStats(String gameId, String playerName) throws RemoteException;

    List<String> getMatchHistory(String gameId, String playerName) throws RemoteException;

    String getOpponentName(String gameId, String playerName) throws RemoteException;
}