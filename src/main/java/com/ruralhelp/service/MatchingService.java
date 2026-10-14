package com.ruralhelp.service;

import com.ruralhelp.dao.AssignmentDAO;
import com.ruralhelp.dao.NgoDAO;
import com.ruralhelp.model.Category;
import com.ruralhelp.model.Issue;
import com.ruralhelp.model.MatchRecommendation;
import com.ruralhelp.model.NGO;
import com.ruralhelp.model.Urgency;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Intelligent Rule-Based Matching Engine (Slide 6 & Slide 10).
 * Matches verified community issues with suitable NGOs based on:
 * 1. Need: Category specialization fit
 * 2. Place: Village/Service area geographic coverage
 * 3. Capacity: Available slots, active workload, and urgency weighting
 * 4. Control: Presents scored recommendations for administrative decision
 */
public class MatchingService {
    private final NgoDAO ngoDAO = new NgoDAO();
    private final AssignmentDAO assignmentDAO = new AssignmentDAO();

    /**
     * Evaluates all registered NGOs and scores them for a specific issue.
     *
     * Scoring Formula:
     * - Need / Category Match:  +45 points (Essential specialization)
     * - Place / Area Coverage:   +35 points (Local village presence)
     * - Capacity Availability:  +20 points (Has >= 1 free slot)
     * - Extra Capacity Buffer:  +10 bonus points (Has >= 3 free slots)
     * - Urgency Multiplier:     For CRITICAL issues, penalizes heavily overloaded NGOs (-30 points)
     * Total possible score: 100 points
     */
    public List<MatchRecommendation> recommendNgosForIssue(Issue issue) throws SQLException {
        List<NGO> allNgos = ngoDAO.findAllNgos();
        List<MatchRecommendation> recommendations = new ArrayList<>();

        Category targetCategory = issue.getCategory();
        String targetArea = issue.getVillageArea();
        Urgency urgency = issue.getUrgency();

        for (NGO ngo : allNgos) {
            int score = 0;
            StringBuilder reasonBuilder = new StringBuilder();

            boolean categoryMatched = ngo.servesCategory(targetCategory);
            boolean areaMatched = ngo.servesArea(targetArea);
            boolean capacityAvailable = ngo.hasAvailableCapacity();

            // 1. Need (Category Specialization)
            if (categoryMatched) {
                score += 45;
                reasonBuilder.append("Category match (").append(targetCategory.getDisplayName()).append("); ");
            } else {
                reasonBuilder.append("No specialized expertise in ").append(targetCategory.getDisplayName()).append("; ");
            }

            // 2. Place (Service Area Coverage)
            if (areaMatched) {
                score += 35;
                reasonBuilder.append("Active presence in ").append(targetArea).append("; ");
            } else {
                reasonBuilder.append("Outside primary service area; ");
            }

            // 3. Capacity
            int remaining = ngo.getRemainingCapacity();
            if (capacityAvailable) {
                score += 20;
                if (remaining >= 3) {
                    score += 10; // high buffer bonus
                    reasonBuilder.append("High bandwidth (").append(remaining).append(" slots available); ");
                } else {
                    reasonBuilder.append("Available bandwidth (").append(remaining).append(" slots left); ");
                }
            } else {
                reasonBuilder.append("At capacity limit (0 free slots); ");
                if (urgency == Urgency.CRITICAL) {
                    score -= 30; // heavy penalty for assigning emergency to full NGO
                } else {
                    score -= 15;
                }
            }

            // Clamp score between 0 and 100
            score = Math.max(0, Math.min(100, score));

            recommendations.add(new MatchRecommendation(
                    ngo,
                    score,
                    categoryMatched,
                    areaMatched,
                    capacityAvailable,
                    reasonBuilder.toString().trim()
            ));
        }

        // Sort descending by score
        Collections.sort(recommendations);
        return recommendations;
    }

    /**
     * Executes assignment of issue to chosen NGO with transactional integrity.
     */
    public boolean allocateIssue(int issueId, int ngoId, int adminUserId, String notes) throws SQLException {
        return assignmentDAO.assignIssueToNgo(issueId, ngoId, adminUserId, notes);
    }

    /**
     * NGO updates the progress of assigned work (e.g. IN_PROGRESS -> RESOLVED).
     */
    public boolean updateWorkStatus(int issueId, int ngoId, int userId, 
                                     com.ruralhelp.model.IssueStatus status, String notes) throws SQLException {
        return assignmentDAO.updateWorkStatus(issueId, ngoId, userId, status, notes);
    }
}

