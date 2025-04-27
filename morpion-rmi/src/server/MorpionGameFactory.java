
// package server;

// import shared.MorpionInterface;
// import java.rmi.RemoteException;
// import java.util.HashMap;
// import java.util.Map;

// public class MorpionGameFactory {
//     private static MorpionGameFactory instance;
//     private final Map<String, MorpionServer> activeGames;

//     private MorpionGameFactory() {
//         activeGames = new HashMap<>();
//     }

//     public static synchronized MorpionGameFactory getInstance() {
//         if (instance == null) {
//             instance = new MorpionGameFactory();
//         }
//         return instance;
//     }

//     public synchronized MorpionInterface getGame(String gameId) throws RemoteException {
//         MorpionServer game = activeGames.get(gameId);
//         if (game == null) {
//             game = new MorpionServer();
//             activeGames.put(gameId, game);
//         }
//         return game;
//     }

//     public synchronized void removeGame(String gameId) {
//         activeGames.remove(gameId);
//     }
// }