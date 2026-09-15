package com.elitelearn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Deck {
    private String name;
    private List<Flashcard> flashcards;
    private transient final Random random;
    
    // NEW: Tracks the last card shown to prevent back-to-back repeats
    private transient Flashcard lastPickedCard;

    // No-arg constructor (Required for Gson Import)
    public Deck() {
        this.flashcards = new ArrayList<>();
        this.random = new Random();
        this.lastPickedCard = null;
    }

    public Deck(String name) {
        this.name = name;
        this.flashcards = new ArrayList<>();
        this.random = new Random();
        this.lastPickedCard = null;
    }

    public Deck(String name, List<Flashcard> flashcards) {
        this.name = name;
        this.flashcards = flashcards != null ? flashcards : new ArrayList<>();
        this.random = new Random();
        this.lastPickedCard = null;
    }

    // --- Deck Management ---
    public void addCard(Flashcard card) { flashcards.add(card); }
    public void addCards(List<Flashcard> cards) { if (cards != null) flashcards.addAll(cards); }
    public boolean isEmpty() { return flashcards.isEmpty(); }
    public int size() { return flashcards.size(); }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Flashcard> getFlashcards() { return flashcards; }

    // --- Study Logic ---
    
    /**
     * Picks a card using a weighted random selection algorithm.
     * GUARANTEE: Will never pick the same card twice in a row (unless the deck only has 1 card).
     */
    public Flashcard pickNextCard() {
        if (isEmpty()) return null;
        
        // Edge Case: If there's only 1 card, it's physically impossible to avoid a repeat.
        if (flashcards.size() == 1) {
            lastPickedCard = flashcards.get(0);
            return lastPickedCard;
        }

        // Safety check: If the last picked card was somehow deleted from the deck
        if (!flashcards.contains(lastPickedCard)) {
            lastPickedCard = null;
        }

        // 1. Calculate total weight EXCLUDING the last picked card
        double totalWeight = 0.0;
        for (Flashcard card : flashcards) {
            if (card != lastPickedCard) {
                totalWeight += card.getWeight();
            }
        }

        // 2. Pick a random number between 0 and the new totalWeight
        double randomValue = random.nextDouble() * totalWeight;

        // 3. Iterate and select, skipping the last picked card
        double cumulativeWeight = 0.0;
        Flashcard pickedCard = null;
        
        for (Flashcard card : flashcards) {
            if (card == lastPickedCard) {
                continue; // Skip the card we just saw
            }
            
            cumulativeWeight += card.getWeight();
            if (cumulativeWeight >= randomValue) {
                pickedCard = card;
                break;
            }
        }

        // Fallback for rare floating-point edge cases
        if (pickedCard == null) {
            for (Flashcard card : flashcards) {
                if (card != lastPickedCard) {
                    pickedCard = card;
                    break;
                }
            }
        }

        // Update the tracker for the next round
        lastPickedCard = pickedCard;
        return pickedCard;
    }

    /**
     * Updates the weight of the card based on user performance.
     */
    public void updateWeight(Flashcard card, boolean isCorrect) {
        if (isCorrect) {
            card.setWeight(Math.max(0.1, card.getWeight() / 1.5)); 
        } else {
            card.setWeight(card.getWeight() * 1.8); 
        }
    }
}