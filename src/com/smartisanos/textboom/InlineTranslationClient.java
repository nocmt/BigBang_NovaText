package com.cashewteam.novatext.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class InlineTranslationClient {
    public static final String DEFAULT_API_URL = "https://api.siliconflow.cn/v1/chat/completions";
    public static final String DEFAULT_MODEL = "tencent/Hunyuan-MT-7B";
    public static final String DEFAULT_PROMPT_TEMPLATE = "将发给你的文本翻译成<目标语言>，不要额外解释。";

    private InlineTranslationClient() {
    }

    public static String translate(
            String text,
            String targetLanguage,
            String apiUrl,
            String apiKey,
            String model,
            String promptTemplate
    ) throws IOException {
        String requestBody = buildChatCompletionBody(model, promptTemplate, targetLanguage, text);
        HttpURLConnection connection = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        byte[] payload = requestBody.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(payload.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(payload);
        }
        try {
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String response = readFully(stream);
            if (status < 200 || status >= 300) {
                throw new IOException("translate http " + status);
            }
            String translated = parseChatCompletionContent(response).trim();
            if (translated.isEmpty()) {
                throw new IOException("empty translation");
            }
            return translated;
        } finally {
            connection.disconnect();
        }
    }

    public static String buildChatCompletionBody(
            String model,
            String promptTemplate,
            String targetLanguage,
            String text
    ) {
        String prompt = promptTemplate.replace("<目标语言>", targetLanguage);
        return "{"
                + "\"model\":\"" + escapeJson(model) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(prompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(text) + "\"}"
                + "]"
                + "}";
    }

    public static String parseChatCompletionContent(String response) {
        if (response == null || response.isEmpty()) {
            return "";
        }
        String key = "\"content\"";
        int keyIndex = response.indexOf(key);
        while (keyIndex >= 0) {
            int colon = response.indexOf(':', keyIndex + key.length());
            if (colon < 0) {
                return "";
            }
            int valueStart = findNextNonWhitespace(response, colon + 1);
            if (valueStart >= 0 && valueStart < response.length() && response.charAt(valueStart) == '"') {
                return parseJsonString(response, valueStart + 1).value;
            }
            keyIndex = response.indexOf(key, colon + 1);
        }
        return "";
    }

    private static int findNextNonWhitespace(String value, int start) {
        for (int i = start; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(c);
                    break;
            }
        }
        return escaped.toString();
    }

    private static ParseResult parseJsonString(String value, int start) {
        StringBuilder decoded = new StringBuilder();
        boolean escaping = false;
        for (int i = start; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                if (c == 'u' && i + 4 < value.length()) {
                    int unicode = parseHexCode(value, i + 1);
                    if (unicode >= 0) {
                        decoded.append((char) unicode);
                        i += 4;
                    } else {
                        decoded.append(c);
                    }
                } else {
                    appendEscaped(decoded, c);
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                return new ParseResult(decoded.toString(), i);
            } else {
                decoded.append(c);
            }
        }
        return new ParseResult(decoded.toString(), value.length() - 1);
    }

    private static void appendEscaped(StringBuilder target, char escaped) {
        switch (escaped) {
            case '"':
            case '\\':
            case '/':
                target.append(escaped);
                break;
            case 'b':
                target.append('\b');
                break;
            case 'f':
                target.append('\f');
                break;
            case 'n':
                target.append('\n');
                break;
            case 'r':
                target.append('\r');
                break;
            case 't':
                target.append('\t');
                break;
            default:
                target.append(escaped);
                break;
        }
    }

    private static int parseHexCode(String value, int start) {
        int result = 0;
        for (int i = start; i < start + 4; i++) {
            int digit = Character.digit(value.charAt(i), 16);
            if (digit < 0) {
                return -1;
            }
            result = (result << 4) + digit;
        }
        return result;
    }

    private static String readFully(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    private static final class ParseResult {
        final String value;
        final int nextIndex;

        ParseResult(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }
}
