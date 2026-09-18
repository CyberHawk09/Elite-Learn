package com.elitelearn;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class ApiKeyManager {
    private static final String KEY_FILE_NAME = ".elitelearn_key";

    public static String getApiKey() {
        try {
            Path path = Paths.get(System.getProperty("user.home"), KEY_FILE_NAME);
            if (Files.exists(path)) {
                String encoded = Files.readString(path).trim();
                return new String(Base64.getDecoder().decode(encoded));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveApiKey(String apiKey) {
        try {
            Path path = Paths.get(System.getProperty("user.home"), KEY_FILE_NAME);
            String encoded = Base64.getEncoder().encodeToString(apiKey.getBytes());
            Files.writeString(path, encoded);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}