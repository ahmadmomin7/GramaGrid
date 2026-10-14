package com.ruralhelp.model;

/**
 * Urgency levels for reported community issues.
 */
public enum Urgency {
    LOW("Low", 20),
    MEDIUM("Medium", 40),
    HIGH("High", 70),
    CRITICAL("Critical", 90);

    private final String displayName;
    private final int baseWeight;

    Urgency(String displayName, int baseWeight) {
        this.displayName = displayName;
        this.baseWeight = baseWeight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBaseWeight() {
        return baseWeight;
    }

    public static Urgency fromString(String text) {
        if (text == null) return MEDIUM;
        for (Urgency u : Urgency.values()) {
            if (u.name().equalsIgnoreCase(text.trim()) || u.displayName.equalsIgnoreCase(text.trim())) {
                return u;
            }
        }
        return MEDIUM;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
