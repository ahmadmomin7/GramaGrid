package com.ruralhelp.dao;

import com.ruralhelp.model.Admin;
import com.ruralhelp.model.CommunityMember;
import com.ruralhelp.model.Role;
import com.ruralhelp.model.User;
import com.ruralhelp.util.DatabaseConnection;
import com.ruralhelp.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User entities using PreparedStatements.
 */
public class UserDAO {

    public User authenticate(String username, String plainPassword) throws SQLException {
        String sql = "SELECT id, username, password_hash, full_name, phone, email, role, village_area, created_at " +
                     "FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (PasswordUtil.verify(plainPassword, storedHash)) {
                        return mapRowToUser(rs);
                    }
                }
            }
        }
        return null;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT id, username, password_hash, full_name, phone, email, role, village_area, created_at " +
                     "FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, full_name, phone, email, role, village_area, created_at " +
                     "FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    public boolean createUser(User user, String plainPassword) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, phone, email, role, village_area) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, PasswordUtil.hash(plainPassword));
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getRole().name());
            ps.setString(7, user.getVillageArea());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public List<User> findAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, full_name, phone, email, role, village_area, created_at " +
                     "FROM users ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
        }
        return list;
    }

    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        Role role = Role.valueOf(rs.getString("role"));
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String fullName = rs.getString("full_name");
        String phone = rs.getString("phone");
        String email = rs.getString("email");
        String villageArea = rs.getString("village_area");
        Timestamp createdAt = rs.getTimestamp("created_at");

        switch (role) {
            case ADMIN:
                return new Admin(id, username, passwordHash, fullName, phone, email, villageArea, createdAt);
            case COMMUNITY_MEMBER:
                return new CommunityMember(id, username, passwordHash, fullName, phone, email, villageArea, createdAt);
            default:
                return new User(id, username, passwordHash, fullName, phone, email, role, villageArea, createdAt);
        }
    }
}
