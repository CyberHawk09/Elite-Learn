package com.elitelearn;
import java.util.List;
import java.util.Random;

public class main{
    private static final Random random = new Random();

    public static void main(String[] args) {
        Flashcard card = pickNextCard(deck, random.nextDouble());
        System.out.println("hello world");
    }

    public static void updateWeight(Flashcard card, boolean isCorrect) {
        if (isCorrect) {
            // Decrease weight, but don't let it drop below 0.1
            card.setWeight(Math.max(0.1, card.getWeight() / 1.5)); 
        } else {
            // Increase weight significantly
            card.setWeight(card.getWeight() * 1.8); 
        }
    }

    public static Flashcard pickNextCard(List<Flashcard> deck, double randomD) {
        if (deck.isEmpty()) return null;

        // 1. Calculate total weight
        double totalWeight = deck.stream().mapToDouble(Flashcard::getWeight).sum();

        // 2. Pick a random number between 0 and totalWeight
        double randomValue = randomD * totalWeight;

        // 3. Iterate through cards and subtract their weight from randomValue
        double cumulativeWeight = 0.0;
        for (Flashcard card : deck) {
            cumulativeWeight += card.getWeight();
            if (cumulativeWeight >= randomValue) {
                return card;
            }
        }

        // Fallback (should theoretically never be reached)
        return deck.get(deck.size() - 1); 
    }
}
