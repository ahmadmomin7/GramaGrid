package com.ruralhelp.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain model representing aggregated dashboard and analytical reports.
 */
public class Report {
    private int totalIssues;
    private int pendingVerificationCount;
    private int verifiedCount;
    private int assignedCount;
    private int inProgressCount;
    private int resolvedCount;
    private int closedCount;
    private int totalNgos;
    private int activeNgos;

    private Map<String, Integer> issuesByCategory = new HashMap<>();
    private Map<String, Integer> issuesByArea = new HashMap<>();
    private Map<String, Integer> issuesByUrgency = new HashMap<>();
    private List<NgoPerformanceRecord> ngoPerformanceList;
    private List<PriorityAlert> priorityAlerts;

    public Report() {}

    public int getTotalIssues() { return totalIssues; }
    public void setTotalIssues(int totalIssues) { this.totalIssues = totalIssues; }

    public int getPendingVerificationCount() { return pendingVerificationCount; }
    public void setPendingVerificationCount(int pendingVerificationCount) { this.pendingVerificationCount = pendingVerificationCount; }

    public int getVerifiedCount() { return verifiedCount; }
    public void setVerifiedCount(int verifiedCount) { this.verifiedCount = verifiedCount; }

    public int getAssignedCount() { return assignedCount; }
    public void setAssignedCount(int assignedCount) { this.assignedCount = assignedCount; }

    public int getInProgressCount() { return inProgressCount; }
    public void setInProgressCount(int inProgressCount) { this.inProgressCount = inProgressCount; }

    public int getResolvedCount() { return resolvedCount; }
    public void setResolvedCount(int resolvedCount) { this.resolvedCount = resolvedCount; }

    public int getClosedCount() { return closedCount; }
    public void setClosedCount(int closedCount) { this.closedCount = closedCount; }

    public int getUnresolvedCount() {
        return pendingVerificationCount + verifiedCount + assignedCount + inProgressCount;
    }

    public int getTotalNgos() { return totalNgos; }
    public void setTotalNgos(int totalNgos) { this.totalNgos = totalNgos; }

    public int getActiveNgos() { return activeNgos; }
    public void setActiveNgos(int activeNgos) { this.activeNgos = activeNgos; }

    public Map<String, Integer> getIssuesByCategory() { return issuesByCategory; }
    public void setIssuesByCategory(Map<String, Integer> issuesByCategory) { this.issuesByCategory = issuesByCategory; }

    public Map<String, Integer> getIssuesByArea() { return issuesByArea; }
    public void setIssuesByArea(Map<String, Integer> issuesByArea) { this.issuesByArea = issuesByArea; }

    public Map<String, Integer> getIssuesByUrgency() { return issuesByUrgency; }
    public void setIssuesByUrgency(Map<String, Integer> issuesByUrgency) { this.issuesByUrgency = issuesByUrgency; }

    public List<NgoPerformanceRecord> getNgoPerformanceList() { return ngoPerformanceList; }
    public void setNgoPerformanceList(List<NgoPerformanceRecord> ngoPerformanceList) { this.ngoPerformanceList = ngoPerformanceList; }

    public List<PriorityAlert> getPriorityAlerts() { return priorityAlerts; }
    public void setPriorityAlerts(List<PriorityAlert> priorityAlerts) { this.priorityAlerts = priorityAlerts; }

    /**
     * Nested class for NGO performance metrics.
     */
    public static class NgoPerformanceRecord {
        private String ngoName;
        private int maxCapacity;
        private int activeCases;
        private int completedCases;
        private double resolutionRate;

        public NgoPerformanceRecord(String ngoName, int maxCapacity, int activeCases, int completedCases) {
            this.ngoName = ngoName;
            this.maxCapacity = maxCapacity;
            this.activeCases = activeCases;
            this.completedCases = completedCases;
            int totalHandled = activeCases + completedCases;
            this.resolutionRate = totalHandled > 0 ? ((double) completedCases / totalHandled) * 100.0 : 0.0;
        }

        public String getNgoName() { return ngoName; }
        public int getMaxCapacity() { return maxCapacity; }
        public int getActiveCases() { return activeCases; }
        public int getCompletedCases() { return completedCases; }
        public double getResolutionRate() { return resolutionRate; }
    }

    /**
     * Nested class for rule-based priority alert items.
     */
    public static class PriorityAlert {
        private String villageArea;
        private String category;
        private int complaintCount;
        private String alertLevel; // WARNING, HIGH, CRITICAL
        private String reason;

        public PriorityAlert(String villageArea, String category, int complaintCount, String alertLevel, String reason) {
            this.villageArea = villageArea;
            this.category = category;
            this.complaintCount = complaintCount;
            this.alertLevel = alertLevel;
            this.reason = reason;
        }

        public String getVillageArea() { return villageArea; }
        public String getCategory() { return category; }
        public int getComplaintCount() { return complaintCount; }
        public String getAlertLevel() { return alertLevel; }
        public String getReason() { return reason; }
    }
}
