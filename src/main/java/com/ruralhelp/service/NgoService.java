package com.ruralhelp.service;

import com.ruralhelp.dao.NgoDAO;
import com.ruralhelp.model.Category;
import com.ruralhelp.model.NGO;

import java.sql.SQLException;
import java.util.List;

/**
 * Service managing NGO operations, specializations, and service areas.
 */
public class NgoService {
    private final NgoDAO ngoDAO = new NgoDAO();

    public List<NGO> getAllNgos() throws SQLException {
        return ngoDAO.findAllNgos();
    }

    public NGO getNgoById(int id) throws SQLException {
        return ngoDAO.findById(id);
    }

    public NGO getNgoByUserId(int userId) throws SQLException {
        return ngoDAO.findByUserId(userId);
    }

    public boolean updateProfile(NGO ngo) throws SQLException {
        return ngoDAO.updateNgoProfile(ngo);
    }

    public void updateSpecializationsAndAreas(int ngoId, List<Category> categories, List<String> areas) throws SQLException {
        ngoDAO.updateSpecializationsAndAreas(ngoId, categories, areas);
    }
}
