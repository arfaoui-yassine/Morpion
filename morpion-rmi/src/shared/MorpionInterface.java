package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface MorpionInterface extends Remote {
    String registerPlayer(String playerName, MorpionCallback callback) throws RemoteException;

    String makeMove(int row, int col, String playerName) throws RemoteException;

    String getCurrentBoard() throws RemoteException;

    boolean isGameOver() throws RemoteException;

    String getWinner() throws RemoteException;

    boolean isPlayerTurn(String playerName) throws RemoteException;

    boolean isGameReady() throws RemoteException;

    void resetGame() throws RemoteException;

    void disconnectPlayer(String playerName) throws RemoteException;

    String getPlayerSymbol(String playerName) throws RemoteException;

    Map<String, Integer> getPlayerStats(String playerName) throws RemoteException;

    List<String> getMatchHistory(String playerName) throws RemoteException;

    String getOpponentName(String playerName) throws RemoteException;

    boolean isPlayerConnected(String playerName) throws RemoteException;
}