package com.ruralhelp.model;

/**
 * System roles for role-based access control.
 */
public enum Role {
    ADMIN("Administrator"),
    NGO("NGO / Organization"),
    COMMUNITY_MEMBER("Community Member");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Role fromString(String text) {
        if (text == null) return null;
        for (Role r : Role.values()) {
            if (r.name().equalsIgnoreCase(text.trim()) || r.displayName.equalsIgnoreCase(text.trim())) {
                return r;
            }
        }
        return null;
    }
}
