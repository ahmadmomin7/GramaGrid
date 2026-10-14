package com.ruralhelp.dao;

import com.ruralhelp.model.Assignment;
import com.ruralhelp.model.IssueStatus;
import com.ruralhelp.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object managing NGO assignments using multi-step transactions.
 */
public class AssignmentDAO {
    private final StatusHistoryDAO statusHistoryDAO = new StatusHistoryDAO();

    public boolean assignIssueToNgo(int issueId, int ngoId, int adminUserId, String notes) throws SQLException {
        String deactivateOldSql = "UPDATE assignments SET status = 'CANCELLED' WHERE issue_id = ? AND status = 'ACTIVE'";
        String insertAssignSql = "INSERT INTO assignments (issue_id, ngo_id, assigned_by, status, notes) VALUES (?, ?, ?, 'ACTIVE', ?)";
        String updateIssueSql = "UPDATE issues SET status = 'ASSIGNED' WHERE id = ?";
        String getPrevStatusSql = "SELECT status FROM issues WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Fetch previous status
            String prevStatus = "VERIFIED";
            try (PreparedStatement psPrev = conn.prepareStatement(getPrevStatusSql)) {
                psPrev.setInt(1, issueId);
                try (ResultSet rs = psPrev.executeQuery()) {
                    if (rs.next()) {
                        prevStatus = rs.getString("status");
                    }
                }
            }

            // 2. Deactivate any prior active assignments for this issue
            try (PreparedStatement psDeact = conn.prepareStatement(deactivateOldSql)) {
                psDeact.setInt(1, issueId);
                psDeact.executeUpdate();
            }

            // 3. Insert new active assignment
            try (PreparedStatement psAssign = conn.prepareStatement(insertAssignSql)) {
                psAssign.setInt(1, issueId);
                psAssign.setInt(2, ngoId);
                psAssign.setInt(3, adminUserId);
                psAssign.setString(4, notes);
                psAssign.executeUpdate();
            }

            // 4. Update issue status to ASSIGNED
            try (PreparedStatement psIssue = conn.prepareStatement(updateIssueSql)) {
                psIssue.setInt(1, issueId);
                psIssue.executeUpdate();
            }

            // 5. Record status history audit trail
            statusHistoryDAO.recordTransition(conn, issueId, adminUserId, prevStatus, 
                    IssueStatus.ASSIGNED.name(), "Assigned to NGO (ID: " + ngoId + "). " + (notes != null ? notes : ""));

            conn.commit(); // Multi-step transaction complete
            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public boolean updateWorkStatus(int issueId, int ngoId, int userId, IssueStatus newStatus, String notes) throws SQLException {
        String updateIssueSql = "UPDATE issues SET status = ? WHERE id = ?";
        String updateAssignSql = "UPDATE assignments SET status = ? WHERE issue_id = ? AND ngo_id = ? AND status = 'ACTIVE'";
        String getPrevStatusSql = "SELECT status FROM issues WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String prevStatus = null;
            try (PreparedStatement psPrev = conn.prepareStatement(getPrevStatusSql)) {
                psPrev.setInt(1, issueId);
                try (ResultSet rs = psPrev.executeQuery()) {
                    if (rs.next()) {
                        prevStatus = rs.getString("status");
                    }
                }
            }

            // 1. Update issue status
            try (PreparedStatement psIssue = conn.prepareStatement(updateIssueSql)) {
                psIssue.setString(1, newStatus.name());
                psIssue.setInt(2, issueId);
                psIssue.executeUpdate();
            }

            // 2. If resolved or closed, complete the assignment
            if (newStatus == IssueStatus.RESOLVED || newStatus == IssueStatus.CLOSED) {
                try (PreparedStatement psAssign = conn.prepareStatement(updateAssignSql)) {
                    psAssign.setString(1, "COMPLETED");
                    psAssign.setInt(2, issueId);
                    psAssign.setInt(3, ngoId);
                    psAssign.executeUpdate();
                }
            }

            // 3. Record status history
            statusHistoryDAO.recordTransition(conn, issueId, userId, prevStatus, newStatus.name(), notes);

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public Assignment findActiveByIssueId(int issueId) throws SQLException {
        String sql = "SELECT a.id, a.issue_id, i.title AS issue_title, a.ngo_id, n.organization_name AS ngo_name, " +
                     "a.assigned_by, u.full_name AS assigned_by_name, a.assigned_at, a.status, a.notes " +
                     "FROM assignments a " +
                     "JOIN issues i ON a.issue_id = i.id " +
                     "JOIN ngos n ON a.ngo_id = n.id " +
                     "JOIN users u ON a.assigned_by = u.id " +
                     "WHERE a.issue_id = ? AND a.status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, issueId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAssignment(rs);
                }
            }
        }
        return null;
    }

    public List<Assignment> findByNgoId(int ngoId) throws SQLException {
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT a.id, a.issue_id, i.title AS issue_title, a.ngo_id, n.organization_name AS ngo_name, " +
                     "a.assigned_by, u.full_name AS assigned_by_name, a.assigned_at, a.status, a.notes " +
                     "FROM assignments a " +
                     "JOIN issues i ON a.issue_id = i.id " +
                     "JOIN ngos n ON a.ngo_id = n.id " +
                     "JOIN users u ON a.assigned_by = u.id " +
                     "WHERE a.ngo_id = ? " +
                     "ORDER BY a.assigned_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ngoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAssignment(rs));
                }
            }
        }
        return list;
    }

    private Assignment mapRowToAssignment(ResultSet rs) throws SQLException {
        Assignment a = new Assignment();
        a.setId(rs.getInt("id"));
        a.setIssueId(rs.getInt("issue_id"));
        a.setIssueTitle(rs.getString("issue_title"));
        a.setNgoId(rs.getInt("ngo_id"));
        a.setNgoName(rs.getString("ngo_name"));
        a.setAssignedBy(rs.getInt("assigned_by"));
        a.setAssignedByName(rs.getString("assigned_by_name"));
        a.setAssignedAt(rs.getTimestamp("assigned_at"));
        a.setStatus(rs.getString("status"));
        a.setNotes(rs.getString("notes"));
        return a;
    }
}
