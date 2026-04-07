package org.engin;

public enum JigglerMode {
    INFINITE("Infinite"),
    FOR_DURATION("For Duration"),
    BETWEEN_HOURS("Between Hours");

    private final String displayName;

    JigglerMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static JigglerMode fromString(String text) {
        for (JigglerMode mode : JigglerMode.values()) {
            if (mode.displayName.equalsIgnoreCase(text)) {
                return mode;
            }
        }
        return INFINITE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
