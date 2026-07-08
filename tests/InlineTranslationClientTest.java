import com.cashewteam.novatext.android.InlineTranslationClient;

public final class InlineTranslationClientTest {
    public static void main(String[] args) {
        String body = InlineTranslationClient.buildChatCompletionBody(
                "tencent/Hunyuan-MT-7B",
                "将发给你的文本翻译成<目标语言>，不要额外解释。",
                "英语",
                "你好\n世界"
        );
        assertTrue(
                body.contains("\"model\":\"tencent/Hunyuan-MT-7B\""),
                "request body contains model"
        );
        assertTrue(
                body.contains("翻译成英语"),
                "request body replaces target language placeholder"
        );
        assertTrue(
                body.contains("你好\\n世界"),
                "request body escapes newlines"
        );
        assertEquals(
                "Hello, world",
                InlineTranslationClient.parseChatCompletionContent(
                        "{\"choices\":[{\"message\":{\"content\":\"Hello, world\"}}]}"
                ),
                "parses chat completion content"
        );
        assertEquals(
                "你好\n世界",
                InlineTranslationClient.parseChatCompletionContent(
                        "{\"choices\":[{\"message\":{\"content\":\"\\u4f60\\u597d\\n\\u4e16\\u754c\"}}]}"
                ),
                "decodes escaped unicode and newline"
        );
        assertEquals(
                "",
                InlineTranslationClient.parseChatCompletionContent("{\"choices\":[]}"),
                "returns empty string when content is unavailable"
        );
    }

    private static void assertEquals(String expected, String actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean actual, String message) {
        if (!actual) {
            throw new AssertionError(message);
        }
    }
}
