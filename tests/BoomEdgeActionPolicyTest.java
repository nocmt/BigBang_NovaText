import com.cashewteam.novatext.android.BoomEdgeActionPolicy;

public final class BoomEdgeActionPolicyTest {
    public static void main(String[] args) {
        assertEquals(
                BoomEdgeActionPolicy.ACTION_CANCEL_SELECTION,
                BoomEdgeActionPolicy.actionForEdgePull(
                        96f,
                        true,
                        0,
                        BoomEdgeActionPolicy.DEFAULT_ACTION_ORDER
                ),
                "first triggered pull cancels selection"
        );
        assertEquals(
                BoomEdgeActionPolicy.ACTION_SELECT_ALL,
                BoomEdgeActionPolicy.actionForEdgePull(
                        96f,
                        true,
                        1,
                        BoomEdgeActionPolicy.DEFAULT_ACTION_ORDER
                ),
                "second triggered pull selects all"
        );
        assertEquals(
                BoomEdgeActionPolicy.ACTION_SELECT_DIGITS,
                BoomEdgeActionPolicy.actionForEdgePull(
                        96f,
                        true,
                        2,
                        BoomEdgeActionPolicy.DEFAULT_ACTION_ORDER
                ),
                "third triggered pull selects continuous digits"
        );
        assertEquals(
                0,
                BoomEdgeActionPolicy.nextIndex(
                        BoomEdgeActionPolicy.DEFAULT_ACTION_ORDER.length - 1,
                        BoomEdgeActionPolicy.DEFAULT_ACTION_ORDER
                ),
                "pull action index wraps"
        );
        assertEquals(
                BoomEdgeActionPolicy.ACTION_NONE,
                BoomEdgeActionPolicy.actionForEdgePull(
                        20f,
                        false,
                        0,
                        BoomEdgeActionPolicy.DEFAULT_ACTION_ORDER
                ),
                "short pull does nothing"
        );
        assertEquals(
                BoomEdgeActionPolicy.defaultActionOrderString(),
                BoomEdgeActionPolicy.normalizeActionOrderString("bad,select_all,select_email"),
                "invalid stored order falls back to default"
        );
        assertTrue(
                BoomEdgeActionPolicy.isLikelyLink("https://tool.oschina.net/regex/"),
                "matches https URL with path"
        );
        assertTrue(
                BoomEdgeActionPolicy.isLikelyLink("http://tool.oschina.net"),
                "matches http URL without path"
        );
        assertTrue(
                BoomEdgeActionPolicy.isLikelyLink("tool.oschina.net"),
                "matches bare domain"
        );
        assertTrue(
                BoomEdgeActionPolicy.isLikelyLink("tool.oschina.net/cd"),
                "matches bare domain with path"
        );
        assertFalse(
                BoomEdgeActionPolicy.isLikelyLink("hello://tool.oschina.net"),
                "does not match arbitrary h-prefixed schemes"
        );
        assertEquals(
                BoomEdgeActionPolicy.DIRECTION_BEFORE,
                BoomEdgeActionPolicy.directionForAdjacentButton(true),
                "previous button requests before text"
        );
        assertEquals(
                BoomEdgeActionPolicy.DIRECTION_AFTER,
                BoomEdgeActionPolicy.directionForAdjacentButton(false),
                "next button requests after text"
        );
    }

    private static void assertEquals(String expected, String actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean actual, String message) {
        if (!actual) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean actual, String message) {
        if (actual) {
            throw new AssertionError(message);
        }
    }
}
