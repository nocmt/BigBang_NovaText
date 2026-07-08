package com.cashewteam.novatext.android;

import java.util.regex.Pattern;

public final class BoomEdgeActionPolicy {
    public static final Pattern LINK_PATTERN = Pattern.compile(
            "(?i)(https?://\\S+|www\\.\\S+|[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(/\\S*)?)"
    );

    public static final String ACTION_NONE = "none";
    public static final String ACTION_CANCEL_SELECTION = "cancel_selection";
    public static final String ACTION_SELECT_ALL = "select_all";
    public static final String ACTION_SELECT_DIGITS = "select_digits";
    public static final String ACTION_SELECT_EMAIL = "select_email";
    public static final String ACTION_SELECT_LINK = "select_link";
    public static final String DIRECTION_BEFORE = "before";
    public static final String DIRECTION_AFTER = "after";
    public static final String[] DEFAULT_ACTION_ORDER = {
            ACTION_SELECT_ALL,
            ACTION_SELECT_DIGITS,
            ACTION_SELECT_EMAIL,
            ACTION_SELECT_LINK,
            ACTION_CANCEL_SELECTION
    };

    private BoomEdgeActionPolicy() {
    }

    public static String actionForEdgePull(
            float offset,
            boolean triggered,
            int currentIndex,
            String[] actionOrder
    ) {
        String[] normalized = normalizeActionOrder(actionOrder);
        if (!triggered || offset == 0f || normalized.length == 0) {
            return ACTION_NONE;
        }
        int safeIndex = Math.max(0, currentIndex) % normalized.length;
        return normalized[safeIndex];
    }

    public static int nextIndex(int currentIndex, String[] actionOrder) {
        String[] normalized = normalizeActionOrder(actionOrder);
        if (normalized.length == 0) {
            return 0;
        }
        return (Math.max(0, currentIndex) + 1) % normalized.length;
    }

    public static int findNextExecutableIndex(
            int currentIndex,
            String[] actionOrder,
            boolean[] executableActions
    ) {
        String[] normalized = normalizeActionOrder(actionOrder);
        if (executableActions == null || executableActions.length != normalized.length) {
            return -1;
        }
        int startIndex = Math.max(0, currentIndex) % normalized.length;
        for (int offset = 0; offset < normalized.length; offset++) {
            int index = (startIndex + offset) % normalized.length;
            if (executableActions[index]) {
                return index;
            }
        }
        return -1;
    }

    public static String defaultActionOrderString() {
        return joinActionOrder(DEFAULT_ACTION_ORDER);
    }

    public static String normalizeActionOrderString(String value) {
        return joinActionOrder(parseActionOrder(value));
    }

    public static String[] parseActionOrder(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_ACTION_ORDER.clone();
        }
        return normalizeActionOrder(value.split(","));
    }

    public static String[] normalizeActionOrder(String[] actionOrder) {
        if (actionOrder == null || actionOrder.length != DEFAULT_ACTION_ORDER.length) {
            return DEFAULT_ACTION_ORDER.clone();
        }
        boolean[] seen = new boolean[DEFAULT_ACTION_ORDER.length];
        String[] normalized = new String[actionOrder.length];
        for (int i = 0; i < actionOrder.length; i++) {
            String action = actionOrder[i] == null ? "" : actionOrder[i].trim();
            int defaultIndex = defaultIndexOf(action);
            if (defaultIndex < 0 || seen[defaultIndex]) {
                return DEFAULT_ACTION_ORDER.clone();
            }
            seen[defaultIndex] = true;
            normalized[i] = action;
        }
        return normalized;
    }

    public static String joinActionOrder(String[] actionOrder) {
        String[] normalized = normalizeActionOrder(actionOrder);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < normalized.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(normalized[i]);
        }
        return builder.toString();
    }

    public static String directionForAdjacentButton(boolean previous) {
        return previous ? DIRECTION_BEFORE : DIRECTION_AFTER;
    }

    public static boolean isLikelyLink(String text) {
        return text != null && LINK_PATTERN.matcher(text).matches();
    }

    private static int defaultIndexOf(String action) {
        for (int i = 0; i < DEFAULT_ACTION_ORDER.length; i++) {
            if (DEFAULT_ACTION_ORDER[i].equals(action)) {
                return i;
            }
        }
        return -1;
    }
}
