package com.cashewteam.novatext.android;

public final class FloatingBallTriggerPolicy {
    public static final int MODE_CLICK = 0;
    public static final int MODE_DOUBLE_CLICK = 1;
    public static final int MODE_DRAG = 2;

    private FloatingBallTriggerPolicy() {
    }

    public static int normalize(int mode) {
        if (mode == MODE_DOUBLE_CLICK || mode == MODE_DRAG) {
            return mode;
        }
        return MODE_CLICK;
    }

    public static boolean shouldCaptureTap(int mode, boolean secondTap) {
        int normalized = normalize(mode);
        if (normalized == MODE_CLICK) {
            return true;
        }
        return normalized == MODE_DOUBLE_CLICK && secondTap;
    }

    public static boolean shouldCaptureDrag(int mode) {
        return normalize(mode) == MODE_DRAG;
    }
}
