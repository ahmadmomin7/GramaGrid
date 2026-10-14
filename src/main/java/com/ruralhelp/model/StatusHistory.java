package com.ruralhelp.model;

import java.sql.Timestamp;

/**
 * Domain model representing an audit record of status progression.
 */
public class StatusHistory {
    private int id;
    private int issueId;
    private int changedBy;
    private String changedByName;
    private String previousStatus;
    private String newStatus;
    private String comments;
    private Timestamp changedAt;

    public StatusHistory() {}

    public StatusHistory(int id, int issueId, int changedBy, String changedByName, 
                         String previousStatus, String newStatus, String comments, Timestamp changedAt) {
        this.id = id;
        this.issueId = issueId;
        this.changedBy = changedBy;
        this.changedByName = changedByName;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.comments = comments;
        this.changedAt = changedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIssueId() { return issueId; }
    public void setIssueId(int issueId) { this.issueId = issueId; }

    public int getChangedBy() { return changedBy; }
    public void setChangedBy(int changedBy) { this.changedBy = changedBy; }

    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Timestamp getChangedAt() { return changedAt; }
    public void setChangedAt(Timestamp changedAt) { this.changedAt = changedAt; }
}
