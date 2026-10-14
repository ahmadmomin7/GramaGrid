package com.ruralhelp.model;

/**
 * Categories of rural issues as defined in the platform specifications:
 * Clean water, sanitation, healthcare, education, and waste management.
 */
public enum Category {
    CLEAN_WATER("Clean Water", "Drinking water sources, pumps, pipelines, contamination"),
    SANITATION("Sanitation", "Toilets, open drainage, sewage overflow, hygiene"),
    HEALTHCARE("Healthcare", "Clinics, emergency medicines, vaccines, doctor availability"),
    EDUCATION("Education", "School infrastructure, teaching staff, classroom safety"),
    WASTE_MANAGEMENT("Waste Management", "Garbage collection, agricultural waste, dumping yards");

    private final String displayName;
    private final String description;

    Category(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static Category fromString(String text) {
        if (text == null) return null;
        for (Category c : Category.values()) {
            if (c.name().equalsIgnoreCase(text.trim()) || c.displayName.equalsIgnoreCase(text.trim())) {
                return c;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
