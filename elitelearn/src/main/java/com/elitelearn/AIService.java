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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AIService {
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String MODEL = "gemini-3.6-flash"; // Updated to a stable flash model
    private final HttpClient httpClient;
    private final Gson gson;

    // --- Feynman Mode Data Classes ---
    public static class FeynmanInit {
        public String concept;
        public String persona;
        public String first_question;
    }

    public static class FeynmanEval {
        public String feedback;
        public String next_question;
    }

    public static class FeynmanResolve {
        public String explanation;
    }

    public static class GradeResult {
        public int score; 
        public String grade; 
        public String explanation;
    }

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.gson = new Gson();
    }

    // ==========================================
    // FEYNMAN MODE METHODS
    // ==========================================
    
    public FeynmanInit initiateFeynmanSession(String rawContext, String apiKey) throws Exception {
        String prompt = """
                You are an expert educational assistant testing a student using the Feynman Technique.
                I will provide some study material. Your goal is to test if the student truly understands it by making them teach it to you.
                
                1. Pick ONE core, fundamental concept from the material.
                2. Adopt a specific, fun persona of a learner who needs this explained (e.g., 'A curious 5-year-old', 'A skeptical college peer', 'A time-traveling historical figure', 'A confused alien').
                3. Ask the student to explain this concept to you in simple terms, staying in character.
                
                You MUST output ONLY a valid JSON object with exactly these three keys:
                - "concept": string (the name of the concept)
                - "persona": string (a short description of your persona, e.g., "Curious 5-Year-Old")
                - "first_question": string (your first question as the persona)
                
                Source material:
                %s
                """.formatted(rawContext);

        JsonObject payload = buildPayload(prompt, null, null);
        String jsonText = sendRequestAndGetText(payload, apiKey);
        return gson.fromJson(jsonText, FeynmanInit.class);
    }

    public FeynmanEval evaluateFeynmanExplanation(String persona, String concept, String previousQuestion, 
                                                  String userExplanation, int currentTurn, String apiKey) throws Exception {
        String prompt = """
                You are continuing a Feynman Technique session. 
                Your Persona: %s
                Concept being taught: %s
                Your previous question: %s
                Student's explanation: %s
                
                This is turn %d out of 3.
                
                Evaluate the student's explanation. Point out any weak points, jargon they used without explaining, or things they missed. 
                Then, ask ONE probing, Socratic follow-up question to test their deeper understanding. Stay in character!
                
                IMPORTANT: If this is turn 3 out of 3, DO NOT ask another question. Instead, give a final, encouraging summary of their teaching and tell them the session is complete.
                
                You MUST output ONLY a valid JSON object with exactly these two keys:
                - "feedback": string (your evaluation and critique)
                - "next_question": string (your follow-up question, OR your final summary if this is turn 3)
                """.formatted(persona, concept, previousQuestion, userExplanation, currentTurn);

        JsonObject payload = buildPayload(prompt, null, null);
        String jsonText = sendRequestAndGetText(payload, apiKey);
        return gson.fromJson(jsonText, FeynmanEval.class);
    }

    public FeynmanResolve resolveFeynmanQuestion(String persona, String concept, String currentQuestion, String apiKey) throws Exception {
        String prompt = """
                The student has raised a white flag and given up on explaining the concept.
                Concept: %s
                Current Question: %s
                
                Drop the persona slightly and explain the concept to the student in a very simple, clear, and easy-to-understand way so they can actually learn it. 
                
                You MUST output ONLY a valid JSON object with exactly this key:
                - "explanation": string (your simple, clear explanation)
                """.formatted(concept, currentQuestion);

        JsonObject payload = buildPayload(prompt, null, null);
        String jsonText = sendRequestAndGetText(payload, apiKey);
        return gson.fromJson(jsonText, FeynmanResolve.class);
    }

    // ==========================================
    // STANDARD FLASHCARD & GRADING METHODS
    // ==========================================

    public List<Flashcard> generateFlashcards(String rawNotes, int numCards, String apiKey) throws Exception {
        String prompt = buildPrompt(numCards, "the following class notes", rawNotes);
        JsonObject payload = buildPayload(prompt, null, null);
        String jsonText = sendRequestAndGetText(payload, apiKey);
        TypeToken<List<Flashcard>> typeToken = new TypeToken<List<Flashcard>>() {};
        return gson.fromJson(jsonText, typeToken.getType());
    }

    public List<Flashcard> generateFlashcardsFromPdfs(List<File> pdfFiles, int numCards, String apiKey) throws Exception {
        String prompt = buildPrompt(numCards, "the provided PDF documents", "");
        List<String> base64Datas = new ArrayList<>();
        for (File pdf : pdfFiles) {
            byte[] fileBytes = Files.readAllBytes(pdf.toPath());
            base64Datas.add(Base64.getEncoder().encodeToString(fileBytes));
        }
        JsonObject payload = buildPayload(prompt, "application/pdf", base64Datas);
        String jsonText = sendRequestAndGetText(payload, apiKey);
        TypeToken<List<Flashcard>> typeToken = new TypeToken<List<Flashcard>>() {};
        return gson.fromJson(jsonText, typeToken.getType());
    }

    public GradeResult gradeAnswer(String question, String expectedAnswer, String userAnswer, String apiKey) throws Exception {
        String prompt = """
                You are an expert grader. Evaluate my answer to a flashcard question.
                Question: %s
                Expected Answer: %s
                My Answer: %s
                Grade my answer with:
                1. A numerical score from 1 to 10.
                2. A categorical grade: exactly one of "CORRECT", "PARTIAL", or "INCORRECT".
                3. A concise explanation focusing specifically on the difference between my answer and the expected answer. Speak directly to me (use "you" and "your").
                You MUST output ONLY a valid JSON object with exactly these three keys:
                - "score": integer (1 to 10)
                - "grade": string ("CORRECT", "PARTIAL", or "INCORRECT")
                - "explanation": string
                """.formatted(question, expectedAnswer, userAnswer);
        JsonObject payload = buildPayload(prompt, null, null);
        String jsonText = sendRequestAndGetText(payload, apiKey);
        return gson.fromJson(jsonText, GradeResult.class);
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

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

    private JsonObject buildPayload(String promptText, String mimeType, List<String> base64Datas) {
        JsonObject payloadObj = new JsonObject();
        JsonArray contentsArray = new JsonArray();
        JsonObject contentObj = new JsonObject();
        contentObj.addProperty("role", "user");
        JsonArray partsArray = new JsonArray();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", promptText);
        partsArray.add(textPart);

        if (mimeType != null && base64Datas != null && !base64Datas.isEmpty()) {
            for (String base64Data : base64Datas) {
                JsonObject inlineData = new JsonObject();
                inlineData.addProperty("mime_type", mimeType);
                inlineData.addProperty("data", base64Data);
                JsonObject dataPart = new JsonObject();
                dataPart.add("inline_data", inlineData);
                partsArray.add(dataPart);
            }
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