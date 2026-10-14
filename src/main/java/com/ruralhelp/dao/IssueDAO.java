package com.ruralhelp.dao;

import com.ruralhelp.model.Category;
import com.ruralhelp.model.Issue;
import com.ruralhelp.model.IssueStatus;
import com.ruralhelp.model.Urgency;
import com.ruralhelp.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Issue entities with JDBC PreparedStatements and transactions.
 */
public class IssueDAO {
    private final StatusHistoryDAO statusHistoryDAO = new StatusHistoryDAO();

    public boolean createIssue(Issue issue) throws SQLException {
        String sql = "INSERT INTO issues (reporter_id, title, category, village_area, urgency, " +
                     "description, photo_path, status, priority_score) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, issue.getReporterId());
                ps.setString(2, issue.getTitle());
                ps.setString(3, issue.getCategory().name());
                ps.setString(4, issue.getVillageArea());
                ps.setString(5, issue.getUrgency().name());
                ps.setString(6, issue.getDescription());
                ps.setString(7, issue.getPhotoPath() != null ? issue.getPhotoPath() : "");
                ps.setString(8, issue.getStatus().name());
                ps.setInt(9, issue.getPriorityScore());

                int affected = ps.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            issue.setId(rs.getInt(1));
                        }
                    }
                } else {
                    conn.rollback();
                    return false;
                }
            }

            // Record initial status history
            statusHistoryDAO.recordTransition(conn, issue.getId(), issue.getReporterId(), 
                    null, issue.getStatus().name(), "Issue reported by community member");

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

    public Issue findById(int id) throws SQLException {
        String sql = "SELECT i.id, i.reporter_id, u.full_name AS reporter_name, u.phone AS reporter_phone, " +
                     "i.title, i.category, i.village_area, i.urgency, i.description, i.photo_path, " +
                     "i.status, i.priority_score, i.created_at, i.updated_at, " +
                     "a.ngo_id AS assigned_ngo_id, n.organization_name AS assigned_ngo_name " +
                     "FROM issues i " +
                     "JOIN users u ON i.reporter_id = u.id " +
                     "LEFT JOIN assignments a ON i.id = a.issue_id AND a.status = 'ACTIVE' " +
                     "LEFT JOIN ngos n ON a.ngo_id = n.id " +
                     "WHERE i.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToIssue(rs);
                }
            }
        }
        return null;
    }

    public List<Issue> findAll() throws SQLException {
        List<Issue> list = new ArrayList<>();
        String sql = "SELECT i.id, i.reporter_id, u.full_name AS reporter_name, u.phone AS reporter_phone, " +
                     "i.title, i.category, i.village_area, i.urgency, i.description, i.photo_path, " +
                     "i.status, i.priority_score, i.created_at, i.updated_at, " +
                     "a.ngo_id AS assigned_ngo_id, n.organization_name AS assigned_ngo_name " +
                     "FROM issues i " +
                     "JOIN users u ON i.reporter_id = u.id " +
                     "LEFT JOIN assignments a ON i.id = a.issue_id AND a.status = 'ACTIVE' " +
                     "LEFT JOIN ngos n ON a.ngo_id = n.id " +
                     "ORDER BY i.priority_score DESC, i.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToIssue(rs));
            }
        }
        return list;
    }

    public List<Issue> findByReporterId(int reporterId) throws SQLException {
        List<Issue> list = new ArrayList<>();
        String sql = "SELECT i.id, i.reporter_id, u.full_name AS reporter_name, u.phone AS reporter_phone, " +
                     "i.title, i.category, i.village_area, i.urgency, i.description, i.photo_path, " +
                     "i.status, i.priority_score, i.created_at, i.updated_at, " +
                     "a.ngo_id AS assigned_ngo_id, n.organization_name AS assigned_ngo_name " +
                     "FROM issues i " +
                     "JOIN users u ON i.reporter_id = u.id " +
                     "LEFT JOIN assignments a ON i.id = a.issue_id AND a.status = 'ACTIVE' " +
                     "LEFT JOIN ngos n ON a.ngo_id = n.id " +
                     "WHERE i.reporter_id = ? " +
                     "ORDER BY i.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reporterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToIssue(rs));
                }
            }
        }
        return list;
    }

    public List<Issue> findByStatus(IssueStatus status) throws SQLException {
        List<Issue> list = new ArrayList<>();
        String sql = "SELECT i.id, i.reporter_id, u.full_name AS reporter_name, u.phone AS reporter_phone, " +
                     "i.title, i.category, i.village_area, i.urgency, i.description, i.photo_path, " +
                     "i.status, i.priority_score, i.created_at, i.updated_at, " +
                     "a.ngo_id AS assigned_ngo_id, n.organization_name AS assigned_ngo_name " +
                     "FROM issues i " +
                     "JOIN users u ON i.reporter_id = u.id " +
                     "LEFT JOIN assignments a ON i.id = a.issue_id AND a.status = 'ACTIVE' " +
                     "LEFT JOIN ngos n ON a.ngo_id = n.id " +
                     "WHERE i.status = ? " +
                     "ORDER BY i.priority_score DESC, i.created_at ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToIssue(rs));
                }
            }
        }
        return list;
    }

    public List<Issue> findByNgoId(int ngoId) throws SQLException {
        List<Issue> list = new ArrayList<>();
        String sql = "SELECT i.id, i.reporter_id, u.full_name AS reporter_name, u.phone AS reporter_phone, " +
                     "i.title, i.category, i.village_area, i.urgency, i.description, i.photo_path, " +
                     "i.status, i.priority_score, i.created_at, i.updated_at, " +
                     "a.ngo_id AS assigned_ngo_id, n.organization_name AS assigned_ngo_name " +
                     "FROM issues i " +
                     "JOIN users u ON i.reporter_id = u.id " +
                     "JOIN assignments a ON i.id = a.issue_id " +
                     "JOIN ngos n ON a.ngo_id = n.id " +
                     "WHERE a.ngo_id = ? " +
                     "ORDER BY (CASE WHEN a.status = 'ACTIVE' THEN 0 ELSE 1 END), i.priority_score DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ngoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToIssue(rs));
                }
            }
        }
        return list;
    }

    public boolean updateStatus(int issueId, IssueStatus newStatus, int changedByUserId, String notes) throws SQLException {
        String fetchPrevSql = "SELECT status FROM issues WHERE id = ?";
        String updateSql = "UPDATE issues SET status = ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String prevStatus = null;
            try (PreparedStatement psFetch = conn.prepareStatement(fetchPrevSql)) {
                psFetch.setInt(1, issueId);
                try (ResultSet rs = psFetch.executeQuery()) {
                    if (rs.next()) {
                        prevStatus = rs.getString("status");
                    }
                }
            }

            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                psUpdate.setString(1, newStatus.name());
                psUpdate.setInt(2, issueId);
                int affected = psUpdate.executeUpdate();
                if (affected <= 0) {
                    conn.rollback();
                    return false;
                }
            }

            statusHistoryDAO.recordTransition(conn, issueId, changedByUserId, prevStatus, newStatus.name(), notes);

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

    public boolean updatePriorityScore(int issueId, int priorityScore) throws SQLException {
        String sql = "UPDATE issues SET priority_score = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, priorityScore);
            ps.setInt(2, issueId);
            return ps.executeUpdate() > 0;
        }
    }

    public int countComplaintsInAreaAndCategory(String villageArea, Category category) throws SQLException {
        String sql = "SELECT COUNT(*) FROM issues WHERE village_area = ? AND category = ? " +
                     "AND status NOT IN ('RESOLVED', 'CLOSED', 'REJECTED')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, villageArea);
            ps.setString(2, category.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Issue mapRowToIssue(ResultSet rs) throws SQLException {
        Issue issue = new Issue();
        issue.setId(rs.getInt("id"));
        issue.setReporterId(rs.getInt("reporter_id"));
        issue.setReporterName(rs.getString("reporter_name"));
        issue.setReporterPhone(rs.getString("reporter_phone"));
        issue.setTitle(rs.getString("title"));
        issue.setCategory(Category.fromString(rs.getString("category")));
        issue.setVillageArea(rs.getString("village_area"));
        issue.setUrgency(Urgency.fromString(rs.getString("urgency")));
        issue.setDescription(rs.getString("description"));
        issue.setPhotoPath(rs.getString("photo_path"));
        issue.setStatus(IssueStatus.fromString(rs.getString("status")));
        issue.setPriorityScore(rs.getInt("priority_score"));
        issue.setCreatedAt(rs.getTimestamp("created_at"));
        issue.setUpdatedAt(rs.getTimestamp("updated_at"));

        int assignedNgoId = rs.getInt("assigned_ngo_id");
        if (!rs.wasNull()) {
            issue.setAssignedNgoId(assignedNgoId);
            issue.setAssignedNgoName(rs.getString("assigned_ngo_name"));
        }
        return issue;
    }
}
