package com.ruralhelp.model;

/**
 * Lifecycle status of an issue from reporting through resolution.
 */
public enum IssueStatus {
    PENDING_VERIFICATION("Pending Verification", "Awaiting admin inspection and approval"),
    VERIFIED("Verified", "Approved by admin, ready for NGO allocation"),
    REJECTED("Rejected", "Declined by admin with reason"),
    ASSIGNED("Assigned", "Allocated to a specialized NGO"),
    IN_PROGRESS("In Progress", "NGO team is actively working on resolution"),
    RESOLVED("Resolved", "Work completed by NGO, awaiting final confirmation"),
    CLOSED("Closed", "Confirmed and officially closed");

    private final String displayName;
    private final String description;

    IssueStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static IssueStatus fromString(String text) {
        if (text == null) return PENDING_VERIFICATION;
        for (IssueStatus s : IssueStatus.values()) {
            if (s.name().equalsIgnoreCase(text.trim()) || s.displayName.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return PENDING_VERIFICATION;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
