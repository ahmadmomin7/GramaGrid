package com.ruralhelp.model;

import java.sql.Timestamp;

/**
 * Domain model representing a reported rural issue.
 */
public class Issue {
    private int id;
    private int reporterId;
    private String reporterName;
    private String reporterPhone;
    private String title;
    private Category category;
    private String villageArea;
    private Urgency urgency;
    private String description;
    private String photoPath;
    private IssueStatus status;
    private int priorityScore;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Joined fields for display convenience
    private Integer assignedNgoId;
    private String assignedNgoName;

    public Issue() {
        this.status = IssueStatus.PENDING_VERIFICATION;
        this.urgency = Urgency.MEDIUM;
        this.priorityScore = 50;
    }

    public Issue(int id, int reporterId, String title, Category category, String villageArea, 
                 Urgency urgency, String description, String photoPath, IssueStatus status, 
                 int priorityScore, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.reporterId = reporterId;
        this.title = title;
        this.category = category;
        this.villageArea = villageArea;
        this.urgency = urgency;
        this.description = description;
        this.photoPath = photoPath;
        this.status = status;
        this.priorityScore = priorityScore;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReporterId() { return reporterId; }
    public void setReporterId(int reporterId) { this.reporterId = reporterId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReporterPhone() { return reporterPhone; }
    public void setReporterPhone(String reporterPhone) { this.reporterPhone = reporterPhone; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getVillageArea() { return villageArea; }
    public void setVillageArea(String villageArea) { this.villageArea = villageArea; }

    public Urgency getUrgency() { return urgency; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public IssueStatus getStatus() { return status; }
    public void setStatus(IssueStatus status) { this.status = status; }

    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public Integer getAssignedNgoId() { return assignedNgoId; }
    public void setAssignedNgoId(Integer assignedNgoId) { this.assignedNgoId = assignedNgoId; }

    public String getAssignedNgoName() { return assignedNgoName; }
    public void setAssignedNgoName(String assignedNgoName) { this.assignedNgoName = assignedNgoName; }

    @Override
    public String toString() {
        return "#" + id + " - " + title + " (" + villageArea + ", " + status + ")";
    }
}
