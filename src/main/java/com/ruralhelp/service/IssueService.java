package com.ruralhelp.service;

import com.ruralhelp.dao.IssueDAO;
import com.ruralhelp.model.Category;
import com.ruralhelp.model.Issue;
import com.ruralhelp.model.IssueStatus;
import com.ruralhelp.model.Urgency;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic service for handling rural community issues,
 * including rule-based priority scoring.
 */
public class IssueService {
    private final IssueDAO issueDAO = new IssueDAO();

    public boolean reportIssue(Issue issue) throws SQLException {
        if (issue.getTitle() == null || issue.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Issue title cannot be empty.");
        }
        if (issue.getCategory() == null) {
            throw new IllegalArgumentException("Issue category must be selected.");
        }
        if (issue.getVillageArea() == null || issue.getVillageArea().trim().isEmpty()) {
            throw new IllegalArgumentException("Village/Area must be provided.");
        }

        // Calculate rule-based priority score
        int score = calculatePriorityScore(issue.getUrgency(), issue.getVillageArea(), issue.getCategory());
        issue.setPriorityScore(score);
        issue.setStatus(IssueStatus.PENDING_VERIFICATION);

        return issueDAO.createIssue(issue);
    }

    /**
     * Rule-based priority prediction engine (Slide 9 & 10):
     * Considers urgency weight + recurrence of problems in the same village/category.
     */
    public int calculatePriorityScore(Urgency urgency, String villageArea, Category category) throws SQLException {
        int baseWeight = urgency != null ? urgency.getBaseWeight() : 40;
        int historicalClusterCount = 0;
        try {
            historicalClusterCount = issueDAO.countComplaintsInAreaAndCategory(villageArea, category);
        } catch (SQLException ignored) {}

        // Each existing unresolved complaint in the same area elevates priority by 10 points
        int clusterBonus = Math.min(30, historicalClusterCount * 10);
        int totalScore = baseWeight + clusterBonus;
        return Math.min(100, Math.max(10, totalScore));
    }

    public List<Issue> getAllIssues() throws SQLException {
        return issueDAO.findAll();
    }

    public Issue getIssueById(int id) throws SQLException {
        return issueDAO.findById(id);
    }

    public List<Issue> getIssuesByReporter(int reporterId) throws SQLException {
        return issueDAO.findByReporterId(reporterId);
    }

    public List<Issue> getPendingIssues() throws SQLException {
        return issueDAO.findByStatus(IssueStatus.PENDING_VERIFICATION);
    }

    public List<Issue> getVerifiedIssues() throws SQLException {
        return issueDAO.findByStatus(IssueStatus.VERIFIED);
    }

    public List<Issue> getIssuesForNgo(int ngoId) throws SQLException {
        return issueDAO.findByNgoId(ngoId);
    }

    public boolean verifyIssue(int issueId, int adminId, String notes) throws SQLException {
        return issueDAO.updateStatus(issueId, IssueStatus.VERIFIED, adminId, 
                notes != null && !notes.isEmpty() ? notes : "Verified by Administrator");
    }

    public boolean rejectIssue(int issueId, int adminId, String reason) throws SQLException {
        return issueDAO.updateStatus(issueId, IssueStatus.REJECTED, adminId, 
                "Rejected: " + (reason != null ? reason : "Insufficient details"));
    }

    public boolean closeIssue(int issueId, int adminId, String notes) throws SQLException {
        return issueDAO.updateStatus(issueId, IssueStatus.CLOSED, adminId, 
                "Closed by Administrator: " + (notes != null ? notes : "Confirmed resolved"));
    }
}
