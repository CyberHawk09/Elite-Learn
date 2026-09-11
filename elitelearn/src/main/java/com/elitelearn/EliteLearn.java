package com.elitelearn;
// EliteLearn.java
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class EliteLearn {
    // Architecture: The app now manages a list of multiple decks!
    private static final List<Deck> decks = new ArrayList<>();
    private static final Scanner scanner = new Scanner(System.in);
    private static final AIService aiService = new AIService();

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("      Welcome to EliteLearn!             ");
        System.out.println("   AI-Powered Smart Flashcard System     ");
        System.out.println("=========================================");

        System.out.print("\nEnter your Google Gemini API Key: ");
        String apiKey = scanner.nextLine();

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    createDeckFromNotes(apiKey);
                    break;
                case "2":
                    studyDeck();
                    break;
                case "3":
                    running = false;
                    System.out.println("Thanks for studying with EliteLearn! Goodbye.");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void printMainMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Create New Deck from Notes");
        System.out.println("2. Study a Deck");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");
    }

    private static void createDeckFromNotes(String apiKey) {
        System.out.print("\nEnter a name for your new deck: ");
        String deckName = scanner.nextLine();

        System.out.println("Enter your class notes below.");
        System.out.println("(Type 'END' on a new line and press Enter to finish):");
        System.out.println("-----------------------------------------");

        StringBuilder notesBuilder = new StringBuilder();
        while (true) {
            String line = scanner.nextLine();
            if (line.equalsIgnoreCase("END")) break;
            notesBuilder.append(line).append("\n");
        }

        try {
            System.out.println("\n* EliteLearn AI is generating your flashcards... *");
            List<Flashcard> generatedCards = aiService.generateFlashcards(notesBuilder.toString(), apiKey);
            
            // Create the new deck and add it to our master list
            Deck newDeck = new Deck(deckName, generatedCards);
            decks.add(newDeck);
            
            System.out.println("* Success! Created deck '" + deckName + "' with " + newDeck.size() + " flashcards. *");
        } catch (Exception e) {
            System.err.println("Error generating flashcards: " + e.getMessage());
        }
    }

    private static void studyDeck() {
        if (decks.isEmpty()) {
            System.out.println("\nYou don't have any decks yet! Create one first.");
            return;
        }

        System.out.println("\n--- Select a Deck to Study ---");
        for (int i = 0; i < decks.size(); i++) {
            System.out.println((i + 1) + ". " + decks.get(i).getName() + " (" + decks.get(i).size() + " cards)");
        }
        System.out.print("Enter deck number (or 0 to cancel): ");

        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        if (choice == 0) return;
        if (choice < 1 || choice > decks.size()) {
            System.out.println("Invalid deck number.");
            return;
        }

        Deck selectedDeck = decks.get(choice - 1);
        runStudySession(selectedDeck);
    }

    private static void runStudySession(Deck deck) {
        System.out.println("\n=========================================");
        System.out.println("Starting study session for: " + deck.getName());
        System.out.println("Press Enter to reveal the answer.");
        System.out.println("=========================================");

        boolean studying = true;
        while (studying && !deck.isEmpty()) {
            // Delegate picking to the Deck class
            Flashcard card = deck.pickNextCard();
            if (card == null) break;

            System.out.println("\n-----------------------------------------");
            System.out.println("QUESTION: " + card.getQuestion());
            System.out.println("-----------------------------------------");
            System.out.print("Press [Enter] to reveal answer (or type 'quit' to exit): ");

            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("quit")) {
                studying = false;
                break;
            }

            System.out.println("\nANSWER: " + card.getAnswer());
            System.out.println("-----------------------------------------");
            System.out.print("Did you get it right? (y/n): ");
            String result = scanner.nextLine();
            boolean isCorrect = result.equalsIgnoreCase("y") || result.equalsIgnoreCase("yes");

            // Delegate weight updating to the Deck class
            deck.updateWeight(card, isCorrect);

            if (isCorrect) {
                System.out.println("✅ Correct! Weight decreased (less likely to appear).");
            } else {
                System.out.println("❌ Incorrect. Weight increased (more likely to appear).");
            }
            System.out.println("Current Weight: " + String.format("%.2f", card.getWeight()));
        }

        System.out.println("\n=========================================");
        System.out.println("Study session ended. Great job!");
        System.out.println("=========================================");
    }
}