package client;

import shared.MorpionInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class MorpionClient {
    private static volatile boolean exiting = false;

    public static void main(String[] args) {
        final String[] playerName = new String[1];
        final MorpionInterface[] gameHolder = new MorpionInterface[1];
        Scanner scanner = new Scanner(System.in);

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            gameHolder[0] = (MorpionInterface) registry.lookup("MorpionGame");

            System.out.print("Enter your name: ");
            playerName[0] = scanner.nextLine();

            // Register shutdown hook for Ctrl+C
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                exiting = true;
                System.out.println("\nDisconnecting from server...");
                try {
                    if (playerName[0] != null && gameHolder[0] != null) {
                        gameHolder[0].disconnectPlayer(playerName[0]);
                    }
                } catch (Exception e) {
                    System.err.println("Error during disconnection: " + e.getMessage());
                }
            }));

            String status = gameHolder[0].registerPlayer(playerName[0]);
            System.out.println(status.equals("WAIT") ? "Waiting for opponent..." : "You are Player O");

            // Wait for game to start
            while (!gameHolder[0].isGameReady() && !exiting) {
                Thread.sleep(2000);
                if (!gameHolder[0].isGameReady()) {
                    System.out.println("Still waiting for opponent...");
                }
            }

            if (exiting)
                return;

            String playerSymbol = gameHolder[0].getPlayerSymbol(playerName[0]);
            System.out.println("Game started! You are Player " + playerSymbol);

            boolean playAgain = true;
            while (playAgain && !exiting) {
                // Game loop
                while (!gameHolder[0].isGameOver() && !exiting) {
                    if (gameHolder[0].isPlayerTurn(playerName[0])) {
                        System.out.println("\nCurrent board:");
                        System.out.println(gameHolder[0].getCurrentBoard());
                        System.out.println("Your turn (Player " + playerSymbol + ")");
                        System.out.print("Enter row and column (0-2) separated by space: ");

                        int row = scanner.nextInt();
                        int col = scanner.nextInt();
                        scanner.nextLine(); // Clear buffer

                        String result = gameHolder[0].makeMove(row, col, playerName[0]);
                        if (!result.equals("VALID_MOVE")) {
                            System.out.println("Invalid move! Try again.");
                        }
                    } else {
                        System.out.println("\nWaiting for opponent's move...");
                        Thread.sleep(2000);
                    }
                }

                if (!exiting) {
                    System.out.println("\nGAME OVER");
                    System.out.println(gameHolder[0].getCurrentBoard());
                    String winner = gameHolder[0].getWinner();
                    System.out.println(winner.equals("Draw") ? "It's a draw!" : "Player " + winner + " wins!");

                    System.out.print("Play again? (y/n): ");
                    String choice = scanner.nextLine();
                    if (choice.equalsIgnoreCase("y")) {
                        gameHolder[0].resetGame();
                        playerSymbol = gameHolder[0].getPlayerSymbol(playerName[0]);
                        System.out.println("New game! You are now Player " + playerSymbol);
                    } else {
                        playAgain = false;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (gameHolder[0] != null && playerName != null) {
                    gameHolder[0].disconnectPlayer(playerName[0]);
                }
                scanner.close();
            } catch (Exception e) {
                System.err.println("Cleanup error: " + e.getMessage());
            }
        }
    }
}