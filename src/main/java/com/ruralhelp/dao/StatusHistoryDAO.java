package com.ruralhelp.dao;

import com.ruralhelp.model.StatusHistory;
import com.ruralhelp.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for recording and retrieving issue status transition histories.
 */
public class StatusHistoryDAO {

    public void recordTransition(Connection conn, int issueId, int changedByUserId, 
                                 String previousStatus, String newStatus, String comments) throws SQLException {
        String sql = "INSERT INTO status_history (issue_id, changed_by, previous_status, new_status, comments) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, issueId);
            ps.setInt(2, changedByUserId);
            ps.setString(3, previousStatus);
            ps.setString(4, newStatus);
            ps.setString(5, comments);
            ps.executeUpdate();
        }
    }

    public List<StatusHistory> findByIssueId(int issueId) throws SQLException {
        List<StatusHistory> list = new ArrayList<>();
        String sql = "SELECT sh.id, sh.issue_id, sh.changed_by, u.full_name AS changed_by_name, " +
                     "sh.previous_status, sh.new_status, sh.comments, sh.changed_at " +
                     "FROM status_history sh " +
                     "JOIN users u ON sh.changed_by = u.id " +
                     "WHERE sh.issue_id = ? " +
                     "ORDER BY sh.changed_at ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, issueId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new StatusHistory(
                            rs.getInt("id"),
                            rs.getInt("issue_id"),
                            rs.getInt("changed_by"),
                            rs.getString("changed_by_name"),
                            rs.getString("previous_status"),
                            rs.getString("new_status"),
                            rs.getString("comments"),
                            rs.getTimestamp("changed_at")
                    ));
                }
            }
        }
        return list;
    }
}
