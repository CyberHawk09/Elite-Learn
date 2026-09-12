package com.elitelearn;
// Flashcard.java
public class Flashcard {
    private String question;
    private String answer;
    private double weight = 1.0; // Changed to double to match getter/setter and Gson expectations

    // No-arg constructor required for Gson deserialization
    public Flashcard() {
        this.weight = 1.0; 
    }

    public Flashcard(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    @Override
    public String toString() {
        return "Flashcard{question='" + question + "', answer='" + answer + "', weight=" + weight + "}";
    }
}