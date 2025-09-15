package com.mangaui.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class DeepLClient {
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String translateToEnglish(String text) throws IOException {
        String apiKey = System.getProperty("DEEPL_API_KEY", System.getenv("DEEPL_API_KEY"));
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("DEEPL_API_KEY is not set. Add it in Settings.");
        }

        String data = "text=" + URLEncoder.encode(text, StandardCharsets.UTF_8) +
                "&target_lang=EN";

        Request request = new Request.Builder()
                .url("https://api-free.deepl.com/v2/translate")
                .addHeader("Authorization", "DeepL-Auth-Key " + apiKey)
                .post(RequestBody.create(data, MediaType.parse("application/x-www-form-urlencoded")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("DeepL API error: " + response.code() + " " + response.message());
            }
            String body = response.body() != null ? response.body().string() : "{}";
            JsonNode node = mapper.readTree(body);
            JsonNode translations = node.path("translations");
            if (translations.isArray() && translations.size() > 0) {
                return translations.get(0).path("text").asText("");
            }
            return "";
        }
    }
}


