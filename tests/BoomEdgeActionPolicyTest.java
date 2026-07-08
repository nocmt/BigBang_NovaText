import com.cashewteam.novatext.android.BoomEdgeActionPolicy;

public final class BoomEdgeActionPolicyTest {
    public static void main(String[] args) {
        assertEquals(
                BoomEdgeActionPolicy.ACTION_SELECT_ALL,
                BoomEdgeActionPolicy.actionForEdgePull(96f, true),
                "triggered pull selects all"
        );
        assertEquals(
                BoomEdgeActionPolicy.ACTION_NONE,
                BoomEdgeActionPolicy.actionForEdgePull(20f, false),
                "short pull does nothing"
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
}
