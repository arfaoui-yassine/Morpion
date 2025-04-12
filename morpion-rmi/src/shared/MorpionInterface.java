package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MorpionInterface extends Remote {
    String registerPlayer(String playerName) throws RemoteException;

    String makeMove(int row, int col, String playerName) throws RemoteException;

    String getCurrentBoard() throws RemoteException;

    boolean isGameOver() throws RemoteException;

    String getWinner() throws RemoteException;

    boolean isPlayerTurn(String playerName) throws RemoteException;

    boolean isGameReady() throws RemoteException;

    void resetGame() throws RemoteException;

    void disconnectPlayer(String playerName) throws RemoteException;

    String getPlayerSymbol(String playerName) throws RemoteException;
}