package com.ruralhelp.model;

import java.sql.Timestamp;

/**
 * Admin domain model representing system coordinators.
 */
public class Admin extends User {

    public Admin() {
        this.role = Role.ADMIN;
    }

    public Admin(int id, String username, String passwordHash, String fullName, 
                 String phone, String email, String officeLocation, Timestamp createdAt) {
        super(id, username, passwordHash, fullName, phone, email, Role.ADMIN, officeLocation, createdAt);
    }
}
