package com.ruralhelp.model;

import java.sql.Timestamp;

/**
 * Domain model representing an assignment of an issue to an NGO.
 */
public class Assignment {
    private int id;
    private int issueId;
    private String issueTitle;
    private int ngoId;
    private String ngoName;
    private int assignedBy;
    private String assignedByName;
    private Timestamp assignedAt;
    private String status; // ACTIVE, COMPLETED, CANCELLED
    private String notes;

    public Assignment() {
        this.status = "ACTIVE";
    }

    public Assignment(int id, int issueId, int ngoId, int assignedBy, 
                      Timestamp assignedAt, String status, String notes) {
        this.id = id;
        this.issueId = issueId;
        this.ngoId = ngoId;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
        this.status = status;
        this.notes = notes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIssueId() { return issueId; }
    public void setIssueId(int issueId) { this.issueId = issueId; }

    public String getIssueTitle() { return issueTitle; }
    public void setIssueTitle(String issueTitle) { this.issueTitle = issueTitle; }

    public int getNgoId() { return ngoId; }
    public void setNgoId(int ngoId) { this.ngoId = ngoId; }

    public String getNgoName() { return ngoName; }
    public void setNgoName(String ngoName) { this.ngoName = ngoName; }

    public int getAssignedBy() { return assignedBy; }
    public void setAssignedBy(int assignedBy) { this.assignedBy = assignedBy; }

    public String getAssignedByName() { return assignedByName; }
    public void setAssignedByName(String assignedByName) { this.assignedByName = assignedByName; }

    public Timestamp getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Timestamp assignedAt) { this.assignedAt = assignedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
