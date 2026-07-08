package com.cashewteam.novatext.android;

public final class BoomEdgeActionPolicy {
    public static final String ACTION_NONE = "none";
    public static final String ACTION_SELECT_ALL = "select_all";
    public static final String DIRECTION_BEFORE = "before";
    public static final String DIRECTION_AFTER = "after";

    private BoomEdgeActionPolicy() {
    }

    public static String actionForEdgePull(float offset, boolean triggered) {
        return triggered && offset != 0f ? ACTION_SELECT_ALL : ACTION_NONE;
    }

    public static String directionForAdjacentButton(boolean previous) {
        return previous ? DIRECTION_BEFORE : DIRECTION_AFTER;
    }
}
