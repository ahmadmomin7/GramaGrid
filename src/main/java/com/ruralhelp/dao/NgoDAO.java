package com.ruralhelp.dao;

import com.ruralhelp.model.Category;
import com.ruralhelp.model.NGO;
import com.ruralhelp.model.Role;
import com.ruralhelp.util.DatabaseConnection;
import com.ruralhelp.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for NGO entities, including specializations and service areas.
 */
public class NgoDAO {

    public NGO findById(int ngoId) throws SQLException {
        String sql = "SELECT n.id AS ngo_id, n.user_id, n.organization_name, n.contact_person, " +
                     "n.phone, n.email, n.max_capacity, n.description, " +
                     "u.username, u.password_hash, u.created_at, " +
                     "COALESCE(vc.active_cases, 0) AS active_cases, " +
                     "COALESCE(vc.completed_cases, 0) AS completed_cases " +
                     "FROM ngos n " +
                     "JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN v_ngo_caseload vc ON n.id = vc.ngo_id " +
                     "WHERE n.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ngoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    NGO ngo = mapRowToNgo(rs);
                    loadSpecializationsAndAreas(conn, ngo);
                    return ngo;
                }
            }
        }
        return null;
    }

    public NGO findByUserId(int userId) throws SQLException {
        String sql = "SELECT n.id AS ngo_id, n.user_id, n.organization_name, n.contact_person, " +
                     "n.phone, n.email, n.max_capacity, n.description, " +
                     "u.username, u.password_hash, u.created_at, " +
                     "COALESCE(vc.active_cases, 0) AS active_cases, " +
                     "COALESCE(vc.completed_cases, 0) AS completed_cases " +
                     "FROM ngos n " +
                     "JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN v_ngo_caseload vc ON n.id = vc.ngo_id " +
                     "WHERE n.user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    NGO ngo = mapRowToNgo(rs);
                    loadSpecializationsAndAreas(conn, ngo);
                    return ngo;
                }
            }
        }
        return null;
    }

    public List<NGO> findAllNgos() throws SQLException {
        List<NGO> list = new ArrayList<>();
        String sql = "SELECT n.id AS ngo_id, n.user_id, n.organization_name, n.contact_person, " +
                     "n.phone, n.email, n.max_capacity, n.description, " +
                     "u.username, u.password_hash, u.created_at, " +
                     "COALESCE(vc.active_cases, 0) AS active_cases, " +
                     "COALESCE(vc.completed_cases, 0) AS completed_cases " +
                     "FROM ngos n " +
                     "JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN v_ngo_caseload vc ON n.id = vc.ngo_id " +
                     "ORDER BY n.organization_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NGO ngo = mapRowToNgo(rs);
                loadSpecializationsAndAreas(conn, ngo);
                list.add(ngo);
            }
        }
        return list;
    }

    public boolean createNgoWithUser(NGO ngo, String plainPassword) throws SQLException {
        String userSql = "INSERT INTO users (username, password_hash, full_name, phone, email, role, village_area) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String ngoSql = "INSERT INTO ngos (user_id, organization_name, contact_person, phone, email, max_capacity, description) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String specSql = "INSERT INTO ngo_specializations (ngo_id, category) VALUES (?, ?)";
        String areaSql = "INSERT INTO ngo_service_areas (ngo_id, village_area) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Insert user
            int userId;
            try (PreparedStatement psUser = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, ngo.getUsername());
                psUser.setString(2, PasswordUtil.hash(plainPassword));
                psUser.setString(3, ngo.getOrganizationName());
                psUser.setString(4, ngo.getPhone());
                psUser.setString(5, ngo.getEmail());
                psUser.setString(6, Role.NGO.name());
                psUser.setString(7, ngo.getVillageArea());
                psUser.executeUpdate();

                try (ResultSet rs = psUser.getGeneratedKeys()) {
                    if (rs.next()) {
                        userId = rs.getInt(1);
                        ngo.setId(userId);
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // 2. Insert ngo profile
            int ngoId;
            try (PreparedStatement psNgo = conn.prepareStatement(ngoSql, Statement.RETURN_GENERATED_KEYS)) {
                psNgo.setInt(1, userId);
                psNgo.setString(2, ngo.getOrganizationName());
                psNgo.setString(3, ngo.getContactPerson());
                psNgo.setString(4, ngo.getPhone());
                psNgo.setString(5, ngo.getEmail());
                psNgo.setInt(6, ngo.getMaxCapacity());
                psNgo.setString(7, ngo.getDescription());
                psNgo.executeUpdate();

                try (ResultSet rs = psNgo.getGeneratedKeys()) {
                    if (rs.next()) {
                        ngoId = rs.getInt(1);
                        ngo.setNgoId(ngoId);
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // 3. Insert specializations
            if (ngo.getSpecializations() != null) {
                try (PreparedStatement psSpec = conn.prepareStatement(specSql)) {
                    for (Category cat : ngo.getSpecializations()) {
                        psSpec.setInt(1, ngoId);
                        psSpec.setString(2, cat.name());
                        psSpec.addBatch();
                    }
                    psSpec.executeBatch();
                }
            }

            // 4. Insert service areas
            if (ngo.getServiceAreas() != null) {
                try (PreparedStatement psArea = conn.prepareStatement(areaSql)) {
                    for (String area : ngo.getServiceAreas()) {
                        if (area != null && !area.trim().isEmpty()) {
                            psArea.setInt(1, ngoId);
                            psArea.setString(2, area.trim());
                            psArea.addBatch();
                        }
                    }
                    psArea.executeBatch();
                }
            }

            conn.commit(); // Commit transaction
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

    public boolean updateNgoProfile(NGO ngo) throws SQLException {
        String sql = "UPDATE ngos SET organization_name = ?, contact_person = ?, phone = ?, " +
                     "email = ?, max_capacity = ?, description = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ngo.getOrganizationName());
            ps.setString(2, ngo.getContactPerson());
            ps.setString(3, ngo.getPhone());
            ps.setString(4, ngo.getEmail());
            ps.setInt(5, ngo.getMaxCapacity());
            ps.setString(6, ngo.getDescription());
            ps.setInt(7, ngo.getNgoId());
            return ps.executeUpdate() > 0;
        }
    }

    public void updateSpecializationsAndAreas(int ngoId, List<Category> categories, List<String> areas) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Clear old
            try (PreparedStatement psDelSpec = conn.prepareStatement("DELETE FROM ngo_specializations WHERE ngo_id = ?");
                 PreparedStatement psDelArea = conn.prepareStatement("DELETE FROM ngo_service_areas WHERE ngo_id = ?")) {
                psDelSpec.setInt(1, ngoId);
                psDelSpec.executeUpdate();
                psDelArea.setInt(1, ngoId);
                psDelArea.executeUpdate();
            }

            // Insert new specializations
            if (categories != null && !categories.isEmpty()) {
                try (PreparedStatement psSpec = conn.prepareStatement("INSERT INTO ngo_specializations (ngo_id, category) VALUES (?, ?)")) {
                    for (Category cat : categories) {
                        psSpec.setInt(1, ngoId);
                        psSpec.setString(2, cat.name());
                        psSpec.addBatch();
                    }
                    psSpec.executeBatch();
                }
            }

            // Insert new areas
            if (areas != null && !areas.isEmpty()) {
                try (PreparedStatement psArea = conn.prepareStatement("INSERT INTO ngo_service_areas (ngo_id, village_area) VALUES (?, ?)")) {
                    for (String area : areas) {
                        if (area != null && !area.trim().isEmpty()) {
                            psArea.setInt(1, ngoId);
                            psArea.setString(2, area.trim());
                            psArea.addBatch();
                        }
                    }
                    psArea.executeBatch();
                }
            }

            conn.commit();
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

    private void loadSpecializationsAndAreas(Connection conn, NGO ngo) throws SQLException {
        // Load specializations
        List<Category> specs = new ArrayList<>();
        String specSql = "SELECT category FROM ngo_specializations WHERE ngo_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(specSql)) {
            ps.setInt(1, ngo.getNgoId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Category cat = Category.fromString(rs.getString("category"));
                    if (cat != null) specs.add(cat);
                }
            }
        }
        ngo.setSpecializations(specs);

        // Load service areas
        List<String> areas = new ArrayList<>();
        String areaSql = "SELECT village_area FROM ngo_service_areas WHERE ngo_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(areaSql)) {
            ps.setInt(1, ngo.getNgoId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    areas.add(rs.getString("village_area"));
                }
            }
        }
        ngo.setServiceAreas(areas);
    }

    private NGO mapRowToNgo(ResultSet rs) throws SQLException {
        NGO ngo = new NGO();
        ngo.setNgoId(rs.getInt("ngo_id"));
        ngo.setId(rs.getInt("user_id"));
        ngo.setUsername(rs.getString("username"));
        ngo.setPasswordHash(rs.getString("password_hash"));
        ngo.setOrganizationName(rs.getString("organization_name"));
        ngo.setContactPerson(rs.getString("contact_person"));
        ngo.setPhone(rs.getString("phone"));
        ngo.setEmail(rs.getString("email"));
        ngo.setMaxCapacity(rs.getInt("max_capacity"));
        ngo.setDescription(rs.getString("description"));
        ngo.setCreatedAt(rs.getTimestamp("created_at"));
        ngo.setActiveCases(rs.getInt("active_cases"));
        ngo.setCompletedCases(rs.getInt("completed_cases"));
        return ngo;
    }
}
