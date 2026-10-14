package com.ruralhelp.model;

import java.sql.Timestamp;

/**
 * Base User domain model.
 */
public class User {
    protected int id;
    protected String username;
    protected String passwordHash;
    protected String fullName;
    protected String phone;
    protected String email;
    protected Role role;
    protected String villageArea;
    protected Timestamp createdAt;

    public User() {}

    public User(int id, String username, String passwordHash, String fullName, String phone, 
                String email, Role role, String villageArea, Timestamp createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.villageArea = villageArea;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getVillageArea() { return villageArea; }
    public void setVillageArea(String villageArea) { this.villageArea = villageArea; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isNgo() { return role == Role.NGO; }
    public boolean isCommunityMember() { return role == Role.COMMUNITY_MEMBER; }

    @Override
    public String toString() {
        return fullName + " (" + (role != null ? role.getDisplayName() : "Unknown") + ")";
    }
}
