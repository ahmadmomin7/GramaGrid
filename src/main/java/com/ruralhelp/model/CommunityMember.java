package com.ruralhelp.model;

import java.sql.Timestamp;

/**
 * Community Member domain model representing rural residents.
 */
public class CommunityMember extends User {

    public CommunityMember() {
        this.role = Role.COMMUNITY_MEMBER;
    }

    public CommunityMember(int id, String username, String passwordHash, String fullName, 
                           String phone, String email, String villageArea, Timestamp createdAt) {
        super(id, username, passwordHash, fullName, phone, email, Role.COMMUNITY_MEMBER, villageArea, createdAt);
    }

    public String getAssignedVillage() {
        return villageArea;
    }

    public void setAssignedVillage(String village) {
        this.villageArea = village;
    }
}
