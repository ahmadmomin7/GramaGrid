package com.ruralhelp.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * NGO domain model representing partner organizations with
 * specializations, service areas, and capacity limits.
 */
public class NGO extends User {
    private int ngoId; // primary key in ngos table
    private String organizationName;
    private String contactPerson;
    private int maxCapacity = 5;
    private int activeCases = 0;
    private int completedCases = 0;
    private String description;
    private List<Category> specializations = new ArrayList<>();
    private List<String> serviceAreas = new ArrayList<>();

    public NGO() {
        this.role = Role.NGO;
    }

    public NGO(int userId, String username, String passwordHash, String fullName, 
               String phone, String email, String villageArea, Timestamp createdAt,
               int ngoId, String organizationName, String contactPerson, int maxCapacity, 
               String description) {
        super(userId, username, passwordHash, fullName, phone, email, Role.NGO, villageArea, createdAt);
        this.ngoId = ngoId;
        this.organizationName = organizationName;
        this.contactPerson = contactPerson;
        this.maxCapacity = maxCapacity;
        this.description = description;
    }

    public int getNgoId() { return ngoId; }
    public void setNgoId(int ngoId) { this.ngoId = ngoId; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }

    public int getActiveCases() { return activeCases; }
    public void setActiveCases(int activeCases) { this.activeCases = activeCases; }

    public int getCompletedCases() { return completedCases; }
    public void setCompletedCases(int completedCases) { this.completedCases = completedCases; }

    public int getRemainingCapacity() {
        return Math.max(0, maxCapacity - activeCases);
    }

    public boolean hasAvailableCapacity() {
        return getRemainingCapacity() > 0;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Category> getSpecializations() { return specializations; }
    public void setSpecializations(List<Category> specializations) { this.specializations = specializations; }

    public List<String> getServiceAreas() { return serviceAreas; }
    public void setServiceAreas(List<String> serviceAreas) { this.serviceAreas = serviceAreas; }

    public boolean servesCategory(Category category) {
        return specializations != null && specializations.contains(category);
    }

    public boolean servesArea(String area) {
        if (serviceAreas == null || area == null) return false;
        for (String sa : serviceAreas) {
            if (sa.equalsIgnoreCase(area.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return organizationName + " (" + getRemainingCapacity() + " free slots)";
    }
}
