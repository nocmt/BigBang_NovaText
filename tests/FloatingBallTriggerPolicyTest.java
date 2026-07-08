import com.cashewteam.novatext.android.FloatingBallTriggerPolicy;

public final class FloatingBallTriggerPolicyTest {
    public static void main(String[] args) {
        assertEquals(
                FloatingBallTriggerPolicy.MODE_CLICK,
                FloatingBallTriggerPolicy.normalize(999),
                "invalid trigger mode falls back to click"
        );
        assertTrue(
                FloatingBallTriggerPolicy.shouldCaptureTap(
                        FloatingBallTriggerPolicy.MODE_CLICK,
                        false
                ),
                "click mode captures every tap"
        );
        assertFalse(
                FloatingBallTriggerPolicy.shouldCaptureTap(
                        FloatingBallTriggerPolicy.MODE_DOUBLE_CLICK,
                        false
                ),
                "double-click mode ignores first tap"
        );
        assertTrue(
                FloatingBallTriggerPolicy.shouldCaptureTap(
                        FloatingBallTriggerPolicy.MODE_DOUBLE_CLICK,
                        true
                ),
                "double-click mode captures second tap"
        );
        assertTrue(
                FloatingBallTriggerPolicy.shouldCaptureDrag(FloatingBallTriggerPolicy.MODE_DRAG),
                "drag mode captures drag release"
        );
        assertFalse(
                FloatingBallTriggerPolicy.shouldCaptureDrag(FloatingBallTriggerPolicy.MODE_CLICK),
                "click mode uses drag for relocation"
        );
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

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }
}
