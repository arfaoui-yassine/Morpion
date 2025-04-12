package server;

import shared.MorpionInterface;
import model.GameState;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MorpionServer extends UnicastRemoteObject implements MorpionInterface {
    private final GameState gameState = new GameState();
    private boolean running = true;

    public MorpionServer() throws RemoteException {
        new Thread(this::cleanupTask).start();
    }

    private void cleanupTask() {
        while (running) {
            try {
                Thread.sleep(50000);
                synchronized (gameState) {
                    if (gameState.checkTimeout()) {
                        System.out.println("Resetting inactive game...");
                        gameState.resetGame();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public synchronized String registerPlayer(String playerName) throws RemoteException {
        return gameState.registerPlayer(playerName);
    }

    @Override
    public synchronized String makeMove(int row, int col, String playerName) throws RemoteException {
        if (!gameState.isGameReady())
            return "GAME_NOT_READY";
        if (gameState.isGameOver())
            return "GAME_OVER";
        if (!gameState.isPlayerTurn(playerName))
            return "NOT_YOUR_TURN";

        return gameState.makeMove(row, col, playerName) ? "VALID_MOVE" : "INVALID_MOVE";
    }

    @Override
    public synchronized String getCurrentBoard() throws RemoteException {
        return gameState.getCurrentBoard();
    }

    @Override
    public synchronized boolean isGameOver() throws RemoteException {
        return gameState.isGameOver();
    }

    @Override
    public synchronized String getWinner() throws RemoteException {
        return gameState.getWinner();
    }

    @Override
    public synchronized boolean isPlayerTurn(String playerName) throws RemoteException {
        return gameState.isPlayerTurn(playerName);
    }

    @Override
    public synchronized boolean isGameReady() throws RemoteException {
        return gameState.isGameReady();
    }

    @Override
    public synchronized void resetGame() throws RemoteException {
        gameState.resetGame();
    }

    @Override
    public synchronized void disconnectPlayer(String playerName) throws RemoteException {
        gameState.disconnectPlayer(playerName);
    }

    @Override
    public synchronized String getPlayerSymbol(String playerName) throws RemoteException {
        return gameState.getPlayerSymbol(playerName);
    }

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("MorpionGame", new MorpionServer());
            System.out.println("Server ready. Waiting for players...");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}