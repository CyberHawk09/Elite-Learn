package com.elitelearn;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

public class AIService {
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String MODEL = "gemini-3.6-flash";
    private final HttpClient httpClient;
    private final Gson gson;
    
    // Simple class to hold the AI's grading response
    public static class GradeResult {
        public int score; // 1 to 10
        public String grade; // "CORRECT", "PARTIAL", or "INCORRECT"
        public String explanation;
    }

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.gson = new Gson();
    }

    // --- Public Methods ---

    public List<Flashcard> generateFlashcards(String rawNotes, int numCards, String apiKey) throws Exception {
        String prompt = buildPrompt(numCards, "the following class notes", rawNotes);
        JsonObject payload = buildPayload(prompt, null, null);
        String jsonText = sendRequestAndGetText(payload, apiKey);
        TypeToken<List<Flashcard>> typeToken = new TypeToken<List<Flashcard>>() {};
        return gson.fromJson(jsonText, typeToken.getType());
    }

    public List<Flashcard> generateFlashcardsFromPdf(File pdfFile, int numCards, String apiKey) throws Exception {
        String prompt = buildPrompt(numCards, "the provided PDF document", "");
        byte[] fileBytes = Files.readAllBytes(pdfFile.toPath());
        String base64Data = Base64.getEncoder().encodeToString(fileBytes);
        JsonObject payload = buildPayload(prompt, "application/pdf", base64Data);
        String jsonText = sendRequestAndGetText(payload, apiKey);
        TypeToken<List<Flashcard>> typeToken = new TypeToken<List<Flashcard>>() {};
        return gson.fromJson(jsonText, typeToken.getType());
    }

    public GradeResult gradeAnswer(String question, String expectedAnswer, String userAnswer, String apiKey) throws Exception {
        String prompt = """
                You are an expert grader. Evaluate the user's answer to a flashcard question.
                
                Question: %s
                Expected Answer: %s
                User's Answer: %s
                
                Grade the user's answer with:
                1. A numerical score from 1 to 10.
                2. A categorical grade: exactly one of "CORRECT", "PARTIAL", or "INCORRECT".
                3. A concise explanation focusing specifically on the difference between the user's answer and the expected answer.
                
                You MUST output ONLY a valid JSON object with exactly these three keys:
                - "score": integer (1 to 10)
                - "grade": string ("CORRECT", "PARTIAL", or "INCORRECT")
                - "explanation": string
                """.formatted(question, expectedAnswer, userAnswer);

        JsonObject payload = buildPayload(prompt, null, null);
        String jsonText = sendRequestAndGetText(payload, apiKey);
        return gson.fromJson(jsonText, GradeResult.class);
    }

    // --- Private Helper Methods ---

    private String buildPrompt(int numCards, String sourceDescription, String rawNotes) {
        return """
                You are an expert educational assistant. Convert %s into exactly %d flashcard-style questions and answers.
                Create comprehensive questions that test understanding, not just memorization.
                
                IMPORTANT FORMATTING RULES:
                - You MUST output ONLY a valid JSON array of objects.
                - Each object must have exactly these two keys: "question" and "answer".
                - Use PLAIN TEXT for all mathematical expressions. 
                - Do NOT use LaTeX, Markdown math formatting, or dollar signs. 
                  (For example: write "f(x) = 2x + 1", NOT "$f(x) = 2x + 1$").
                
                Source material:
                %s
                """.formatted(sourceDescription, numCards, rawNotes);
    }

    private JsonObject buildPayload(String promptText, String mimeType, String base64Data) {
        JsonObject payloadObj = new JsonObject();
        JsonArray contentsArray = new JsonArray();
        JsonObject contentObj = new JsonObject();
        contentObj.addProperty("role", "user");
        JsonArray partsArray = new JsonArray();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", promptText);
        partsArray.add(textPart);

        if (mimeType != null && base64Data != null) {
            JsonObject inlineData = new JsonObject();
            inlineData.addProperty("mime_type", mimeType);
            inlineData.addProperty("data", base64Data);
            JsonObject dataPart = new JsonObject();
            dataPart.add("inline_data", inlineData);
            partsArray.add(dataPart);
        }

        contentObj.add("parts", partsArray);
        contentsArray.add(contentObj);
        payloadObj.add("contents", contentsArray);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("responseMimeType", "application/json"); 
        payloadObj.add("generationConfig", generationConfig);

        return payloadObj;
    }

    // Refactored to return the raw JSON text so it can be parsed into different objects
    private String sendRequestAndGetText(JsonObject payloadObj, String apiKey) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Google API Key cannot be empty.");
        }

        String jsonPayload = gson.toJson(payloadObj);
        String fullUrl = GEMINI_BASE_URL + MODEL + ":generateContent?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String errorMsg = "API Error (" + response.statusCode() + "): ";
            try {
                JsonObject errorJson = JsonParser.parseString(response.body()).getAsJsonObject();
                errorMsg += errorJson.getAsJsonObject("error").get("message").getAsString();
            } catch (Exception e) {
                errorMsg += response.body();
            }
            throw new RuntimeException(errorMsg);
        }

        return extractContentFromGeminiResponse(response.body());
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