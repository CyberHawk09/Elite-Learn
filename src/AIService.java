// AIService.java
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class AIService {
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String MODEL = "gemini-1.5-flash"; 
    private final HttpClient httpClient;
    private final Gson gson;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.gson = new Gson();
    }

    public List<Flashcard> generateFlashcards(String rawNotes, String apiKey) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Google API Key cannot be empty.");
        }

        // ENHANCEMENT: Explicitly tell the AI the exact JSON structure we need
        String prompt = """
                You are an expert educational assistant. Convert the following class notes into flashcard-style questions and answers.
                Create comprehensive questions that test understanding, not just memorization.
                
                You MUST output ONLY a valid JSON array of objects. 
                Each object in the array must have exactly these three keys:
                - "question": string
                - "answer": string
                - "weight": number (always set to 1.0 initially)
                
                Example output:
                [
                  {"question": "What is the capital of France?", "answer": "Paris", "weight": 1.0}
                ]
                
                Notes:
                %s
                """.formatted(rawNotes);

        JsonObject payloadObj = new JsonObject();
        JsonArray contentsArray = new JsonArray();
        JsonObject contentObj = new JsonObject();
        contentObj.addProperty("role", "user");
        JsonArray partsArray = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        partsArray.add(textPart);
        contentObj.add("parts", partsArray);
        contentsArray.add(contentObj);
        payloadObj.add("contents", contentsArray);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("responseMimeType", "application/json"); 
        payloadObj.add("generationConfig", generationConfig);

        String jsonPayload = gson.toJson(payloadObj);
        String fullUrl = GEMINI_BASE_URL + MODEL + ":generateContent?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String errorMsg = "API Error (" + response.statusCode() + "): " + response.body();
            throw new RuntimeException(errorMsg);
        }

        String aiJsonText = extractContentFromGeminiResponse(response.body());
        TypeToken<List<Flashcard>> typeToken = new TypeToken<List<Flashcard>>() {};
        return gson.fromJson(aiJsonText, typeToken.getType());
    }

    private String extractContentFromGeminiResponse(String jsonResponse) {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            if (root.has("promptFeedback")) {
                JsonObject feedback = root.getAsJsonObject("promptFeedback");
                if (feedback.has("blockReason")) {
                    throw new RuntimeException("Prompt blocked: " + feedback.get("blockReason").getAsString());
                }
            }
            return root.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response. Raw: " + jsonResponse, e);
        }
    }
}