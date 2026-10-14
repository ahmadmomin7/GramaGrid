package com.ruralhelp.service;

import com.ruralhelp.dao.NgoDAO;
import com.ruralhelp.dao.UserDAO;
import com.ruralhelp.model.CommunityMember;
import com.ruralhelp.model.NGO;
import com.ruralhelp.model.Role;
import com.ruralhelp.model.User;

import java.sql.SQLException;

/**
 * Authentication and User session service.
 */
public class AuthService {
    private final UserDAO userDAO = new UserDAO();
    private final NgoDAO ngoDAO = new NgoDAO();
    private User currentUser;

    public User login(String username, String password) throws SQLException {
        if (username == null || username.trim().isEmpty() || password == null) {
            return null;
        }
        User user = userDAO.authenticate(username.trim(), password);
        if (user != null) {
            if (user.getRole() == Role.NGO) {
                // Load complete NGO profile
                NGO ngo = ngoDAO.findByUserId(user.getId());
                if (ngo != null) {
                    this.currentUser = ngo;
                    return ngo;
                }
            }
            this.currentUser = user;
        }
        return user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean registerCommunityMember(String username, String password, String fullName, 
                                           String phone, String email, String village) throws SQLException {
        if (userDAO.existsByUsername(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        CommunityMember member = new CommunityMember();
        member.setUsername(username.trim());
        member.setFullName(fullName.trim());
        member.setPhone(phone != null ? phone.trim() : "");
        member.setEmail(email != null ? email.trim() : "");
        member.setVillageArea(village != null ? village.trim() : "");
        member.setRole(Role.COMMUNITY_MEMBER);

        return userDAO.createUser(member, password);
    }

    public boolean registerNgo(NGO ngo, String password) throws SQLException {
        if (userDAO.existsByUsername(ngo.getUsername())) {
            throw new IllegalArgumentException("Username '" + ngo.getUsername() + "' is already taken.");
        }
        return ngoDAO.createNgoWithUser(ngo, password);
    }
}
