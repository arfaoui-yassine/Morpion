// server/MorpionServer.java
package server;

import shared.MorpionInterface;
import shared.MorpionCallback;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.List;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class MorpionServer extends RemoteServer implements MorpionInterface {
    private final MorpionGameFactory gameFactory;

    public MorpionServer() throws RemoteException {
        super();
        this.gameFactory = new MorpionGameFactory();
    }

    @Override
    public String createNewGame() throws RemoteException {
        return gameFactory.createNewGame();
    }

    @Override
    public List<String> getAvailableGames() throws RemoteException {
        return gameFactory.getAvailableGames();
    }

    @Override
    public String registerPlayer(String gameId, String playerName, MorpionCallback callback) throws RemoteException {
        return gameFactory.registerPlayer(gameId, playerName, callback);
    }

    @Override
    public String makeMove(String gameId, int row, int col, String playerName) throws RemoteException {
        return gameFactory.makeMove(gameId, row, col, playerName);
    }

    @Override
    public String getCurrentBoard(String gameId) throws RemoteException {
        return gameFactory.getCurrentBoard(gameId);
    }

    @Override
    public boolean isGameOver(String gameId) throws RemoteException {
        return gameFactory.isGameOver(gameId);
    }

    @Override
    public String getWinner(String gameId) throws RemoteException {
        return gameFactory.getWinner(gameId);
    }

    @Override
    public boolean isPlayerTurn(String gameId, String playerName) throws RemoteException {
        return gameFactory.isPlayerTurn(gameId, playerName);
    }

    @Override
    public boolean isGameReady(String gameId) throws RemoteException {
        return gameFactory.isGameReady(gameId);
    }

    @Override
    public void resetGame(String gameId) throws RemoteException {
        gameFactory.resetGame(gameId);
    }

    @Override
    public void disconnectPlayer(String gameId, String playerName) throws RemoteException {
        gameFactory.disconnectPlayer(gameId, playerName);
    }

    @Override
    public String getPlayerSymbol(String gameId, String playerName) throws RemoteException {
        return gameFactory.getPlayerSymbol(gameId, playerName);
    }

    @Override
    public Map<String, Integer> getPlayerStats(String gameId, String playerName) throws RemoteException {
        return gameFactory.getPlayerStats(gameId, playerName);
    }

    @Override
    public List<String> getMatchHistory(String gameId, String playerName) throws RemoteException {
        return gameFactory.getMatchHistory(gameId, playerName);
    }

    @Override
    public String getOpponentName(String gameId, String playerName) throws RemoteException {
        return gameFactory.getOpponentName(gameId, playerName);
    }

    public static void main(String[] args) {
        try {
            MorpionServer server = new MorpionServer();
            MorpionInterface stub = (MorpionInterface) UnicastRemoteObject.exportObject(server, 0);

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("MorpionGame", stub);

            System.out.println("Server ready. Waiting for players...");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}