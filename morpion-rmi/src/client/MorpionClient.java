package client;

import shared.MorpionInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.Map;
import java.util.List;

public class MorpionClient {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter your name: ");
            String playerName = scanner.nextLine();

            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            MorpionInterface game = (MorpionInterface) registry.lookup("MorpionGame");

            // String status = game.registerPlayer(playerName);
            // System.out.println(status.equals("WAIT") ? "Waiting for opponent..." :
            // "Connected to game");

            while (!game.isGameReady()) {
                Thread.sleep(2000);
                System.out.println("Waiting for opponent...");
            }

            String playerSymbol = game.getPlayerSymbol(playerName);
            System.out.println("Game started! You are Player " + playerSymbol);
            printStats(game, playerName);

            while (!game.isGameOver()) {
                System.out.println("\nCurrent board:");
                System.out.println(game.getCurrentBoard());

                if (game.isPlayerTurn(playerName)) {
                    System.out.print("Your turn (row[0-2] column[0-2]): ");
                    int row = scanner.nextInt();
                    int col = scanner.nextInt();
                    scanner.nextLine(); // consume newline

                    String result = game.makeMove(row, col, playerName);
                    if (!result.equals("VALID_MOVE")) {
                        System.out.println("Invalid move: " + result);
                    }
                } else {
                    System.out.println("Waiting for opponent's move...");
                    Thread.sleep(2000);
                }
            }

            // Game over
            System.out.println("\nFinal board:");
            System.out.println(game.getCurrentBoard());
            String winner = game.getWinner();
            System.out.println(winner.equals("DRAW") ? "Game ended in a draw!"
                    : winner.equals(playerName) ? "You won!" : "You lost!");

            printStats(game, playerName);

            System.out.print("Play again? (y/n): ");
            if (scanner.nextLine().equalsIgnoreCase("y")) {
                game.resetGame();
                main(args);
            } else {
                game.disconnectPlayer(playerName);
            }

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printStats(MorpionInterface game, String playerName) throws Exception {
        Map<String, Integer> stats = game.getPlayerStats(playerName);
        System.out.println("\nYour stats - Wins: " + stats.get("wins") +
                " | Losses: " + stats.get("losses") +
                " | Draws: " + stats.get("draws"));

        List<String> history = game.getMatchHistory(playerName);
        if (!history.isEmpty()) {
            System.out.println("\nMatch History:");
            for (String match : history) {
                System.out.println(" - " + match);
            }
        }
    }
}