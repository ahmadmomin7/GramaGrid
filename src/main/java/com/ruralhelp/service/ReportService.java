package com.ruralhelp.service;

import com.ruralhelp.dao.ReportDAO;
import com.ruralhelp.model.Report;

import java.sql.SQLException;

/**
 * Service for reporting and dashboard aggregation.
 */
public class ReportService {
    private final ReportDAO reportDAO = new ReportDAO();

    public Report getDashboardReport() throws SQLException {
        return reportDAO.generateReport();
    }
}

