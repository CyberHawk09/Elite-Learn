package com.elitelearn;
// Deck.java
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Deck {
    private String name;
    private List<Flashcard> flashcards;
    private final Random random;

    public Deck(String name) {
        this.name = name;
        this.flashcards = new ArrayList<>();
        this.random = new Random();
    }

    public Deck(String name, List<Flashcard> flashcards) {
        this.name = name;
        this.flashcards = flashcards != null ? flashcards : new ArrayList<>();
        this.random = new Random();
    }

    // --- Deck Management ---
    public void addCard(Flashcard card) {
        flashcards.add(card);
    }

    public void addCards(List<Flashcard> cards) {
        if (cards != null) {
            flashcards.addAll(cards);
        }
    }

    public boolean isEmpty() {
        return flashcards.isEmpty();
    }

    public int size() {
        return flashcards.size();
    }

    public String getName() {
        return name;
    }

    // --- Study Logic ---
    
    /**
     * Picks a card using a weighted random selection algorithm.
     * Cards with higher weights (answered incorrectly) are more likely to be picked.
     */
    public Flashcard pickNextCard() {
        if (isEmpty()) return null;

        // 1. Calculate total weight
        double totalWeight = flashcards.stream().mapToDouble(Flashcard::getWeight).sum();
        
        // 2. Pick a random number between 0 and totalWeight
        double randomValue = random.nextDouble() * totalWeight;
        
        // 3. Iterate through cards and subtract their weight from randomValue
        double cumulativeWeight = 0.0;
        for (Flashcard card : flashcards) {
            cumulativeWeight += card.getWeight();
            if (cumulativeWeight >= randomValue) {
                return card;
            }
        }
        
        // Fallback (should theoretically never be reached)
        return flashcards.get(flashcards.size() - 1); 
    }

    /**
     * Updates the weight of the card based on user performance.
     */
    public void updateWeight(Flashcard card, boolean isCorrect) {
        if (isCorrect) {
            // Decrease weight, but don't let it drop below 0.1
            card.setWeight(Math.max(0.1, card.getWeight() / 1.5)); 
        } else {
            // Increase weight significantly
            card.setWeight(card.getWeight() * 1.8); 
        }
    }
}