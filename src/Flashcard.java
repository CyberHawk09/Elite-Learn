// Flashcard.java
public class Flashcard {
    private String question;
    private String answer;
    private float weight;

    public Flashcard(String question, String answer, float weight) {
        this.question = question;
        this.answer = answer;
        this.weight = weight;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "Flashcard{question='" + question + "', answer='" + answer + "', weight=" + weight + "}";
    }
}