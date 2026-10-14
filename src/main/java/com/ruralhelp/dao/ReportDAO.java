package com.ruralhelp.dao;

import com.ruralhelp.model.Report;
import com.ruralhelp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for generating reports and dashboard metrics
 * utilizing SQL JOIN, COUNT, GROUP BY, and ORDER BY queries.
 */
public class ReportDAO {

    public Report generateReport() throws SQLException {
        Report report = new Report();

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Overall counts by status
            String statusCountSql = "SELECT status, COUNT(*) AS cnt FROM issues GROUP BY status";
            try (PreparedStatement ps = conn.prepareStatement(statusCountSql);
                 ResultSet rs = ps.executeQuery()) {
                int total = 0;
                while (rs.next()) {
                    String status = rs.getString("status");
                    int count = rs.getInt("cnt");
                    total += count;
                    switch (status) {
                        case "PENDING_VERIFICATION": report.setPendingVerificationCount(count); break;
                        case "VERIFIED": report.setVerifiedCount(count); break;
                        case "ASSIGNED": report.setAssignedCount(count); break;
                        case "IN_PROGRESS": report.setInProgressCount(count); break;
                        case "RESOLVED": report.setResolvedCount(count); break;
                        case "CLOSED": report.setClosedCount(count); break;
                    }
                }
                report.setTotalIssues(total);
            }

            // 2. Issues by category (COUNT, GROUP BY, ORDER BY)
            String categorySql = "SELECT category, COUNT(*) AS cnt FROM issues GROUP BY category ORDER BY cnt DESC";
            Map<String, Integer> catMap = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(categorySql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    catMap.put(rs.getString("category"), rs.getInt("cnt"));
                }
            }
            report.setIssuesByCategory(catMap);

            // 3. Issues by village area (COUNT, GROUP BY, ORDER BY)
            String areaSql = "SELECT village_area, COUNT(*) AS cnt FROM issues GROUP BY village_area ORDER BY cnt DESC";
            Map<String, Integer> areaMap = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(areaSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    areaMap.put(rs.getString("village_area"), rs.getInt("cnt"));
                }
            }
            report.setIssuesByArea(areaMap);

            // 4. Issues by urgency
            String urgencySql = "SELECT urgency, COUNT(*) AS cnt FROM issues GROUP BY urgency ORDER BY cnt DESC";
            Map<String, Integer> urgMap = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(urgencySql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    urgMap.put(rs.getString("urgency"), rs.getInt("cnt"));
                }
            }
            report.setIssuesByUrgency(urgMap);

            // 5. NGO Performance metrics (JOIN, COUNT, GROUP BY, ORDER BY)
            String ngoPerfSql = "SELECT n.organization_name, n.max_capacity, " +
                                "COUNT(CASE WHEN a.status = 'ACTIVE' THEN 1 END) AS active_cases, " +
                                "COUNT(CASE WHEN a.status = 'COMPLETED' THEN 1 END) AS completed_cases " +
                                "FROM ngos n " +
                                "LEFT JOIN assignments a ON n.id = a.ngo_id " +
                                "GROUP BY n.id, n.organization_name, n.max_capacity " +
                                "ORDER BY completed_cases DESC, active_cases DESC";
            List<Report.NgoPerformanceRecord> perfList = new ArrayList<>();
            int totalNgos = 0;
            int activeNgos = 0;
            try (PreparedStatement ps = conn.prepareStatement(ngoPerfSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totalNgos++;
                    int active = rs.getInt("active_cases");
                    if (active > 0) activeNgos++;
                    perfList.add(new Report.NgoPerformanceRecord(
                            rs.getString("organization_name"),
                            rs.getInt("max_capacity"),
                            active,
                            rs.getInt("completed_cases")
                    ));
                }
            }
            report.setTotalNgos(totalNgos);
            report.setActiveNgos(activeNgos);
            report.setNgoPerformanceList(perfList);

            // 6. Priority Alerts (detect hotspots: unresolved count by village & category)
            String hotspotSql = "SELECT village_area, category, COUNT(*) AS cnt, " +
                                "SUM(CASE WHEN urgency = 'CRITICAL' THEN 1 ELSE 0 END) AS critical_cnt " +
                                "FROM issues " +
                                "WHERE status NOT IN ('RESOLVED', 'CLOSED', 'REJECTED') " +
                                "GROUP BY village_area, category " +
                                "HAVING cnt >= 2 OR critical_cnt >= 1 " +
                                "ORDER BY critical_cnt DESC, cnt DESC";
            List<Report.PriorityAlert> alerts = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(hotspotSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String village = rs.getString("village_area");
                    String category = rs.getString("category");
                    int count = rs.getInt("cnt");
                    int criticalCount = rs.getInt("critical_cnt");

                    String level = criticalCount > 0 ? "CRITICAL" : (count >= 3 ? "HIGH" : "WARNING");
                    String reason = criticalCount > 0 
                            ? "Contains " + criticalCount + " active critical emergency complaint(s)!" 
                            : "Cluster of " + count + " unresolved complaints detected in this sector.";

                    alerts.add(new Report.PriorityAlert(village, category, count, level, reason));
                }
            }
            report.setPriorityAlerts(alerts);
        }

        return report;
    }
}
