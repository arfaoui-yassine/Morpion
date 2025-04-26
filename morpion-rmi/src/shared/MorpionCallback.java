
package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MorpionCallback extends Remote {
    void updateBoard(String boardState) throws RemoteException;

    void gameReady(String playerSymbol) throws RemoteException;

    void gameOver(String winner) throws RemoteException;

    void opponentDisconnected() throws RemoteException;
}