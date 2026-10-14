package com.ruralhelp.test;

import com.ruralhelp.dao.StatusHistoryDAO;
import com.ruralhelp.model.*;
import com.ruralhelp.service.*;
import com.ruralhelp.util.DatabaseConnection;

import java.util.List;

/**
 * Automated end-to-end integration test verifying all 6 modules and JDBC operations.
 */
public class PlatformIntegrationTest {

    public static void main(String[] args) {
        System.out.println("==============================================================");
        System.out.println("   RURAL COMMUNITY SUPPORT PLATFORM - AUTOMATED VERIFICATION  ");
        System.out.println("==============================================================\n");

        int passed = 0;
        int failed = 0;

        AuthService authService = new AuthService();
        IssueService issueService = new IssueService();
        MatchingService matchingService = new MatchingService();
        NgoService ngoService = new NgoService();
        ReportService reportService = new ReportService();
        StatusHistoryDAO statusHistoryDAO = new StatusHistoryDAO();

        // 1. Test Database Connectivity
        try {
            System.out.print("[TEST 1/8] Verifying Database Connection... ");
            if (DatabaseConnection.testConnection()) {
                System.out.println("PASSED! Connected to MySQL.");
                passed++;
            } else {
                System.out.println("FAILED!");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // 2. Test User Authentication (RBAC)
        User adminUser = null;
        User ngoUser = null;
        User memberUser = null;
        try {
            System.out.print("[TEST 2/8] Testing Authentication & RBAC Roles... ");
            adminUser = authService.login("admin", "admin123");
            ngoUser = authService.login("jalseva_ngo", "ngo123");
            memberUser = authService.login("ramesh_k", "user123");

            if (adminUser != null && adminUser.isAdmin() &&
                ngoUser != null && ngoUser.isNgo() &&
                memberUser != null && memberUser.isCommunityMember()) {
                System.out.println("PASSED! (Admin, NGO, Member authenticated)");
                passed++;
            } else {
                System.out.println("FAILED! Invalid credentials or role mapping.");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // 3. Test Issue Reporting & Rule-Based Priority Score
        Issue newIssue = null;
        try {
            System.out.print("[TEST 3/8] Testing Issue Reporting & Priority Scoring... ");
            newIssue = new Issue();
            newIssue.setReporterId(memberUser.getId());
            newIssue.setTitle("Water Pipeline Burst near Community Hall");
            newIssue.setCategory(Category.CLEAN_WATER);
            newIssue.setVillageArea("Rampur");
            newIssue.setUrgency(Urgency.HIGH);
            newIssue.setDescription("Pipeline burst causing severe water shortage in sector 4.");

            boolean reported = issueService.reportIssue(newIssue);
            if (reported && newIssue.getId() > 0 && newIssue.getPriorityScore() >= 70) {
                System.out.println("PASSED! Created Issue #" + newIssue.getId() + " (Priority: " + newIssue.getPriorityScore() + ")");
                passed++;
            } else {
                System.out.println("FAILED!");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // 4. Test Admin Verification Workflow
        try {
            System.out.print("[TEST 4/8] Testing Admin Verification Workflow... ");
            boolean verified = issueService.verifyIssue(newIssue.getId(), adminUser.getId(), "Cross-verified with Rampur Panchayat Secretary");
            Issue reloaded = issueService.getIssueById(newIssue.getId());
            if (verified && reloaded.getStatus() == IssueStatus.VERIFIED) {
                System.out.println("PASSED! Status transitioned to VERIFIED.");
                passed++;
            } else {
                System.out.println("FAILED!");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // 5. Test Intelligent Matching Engine (Need + Place + Capacity)
        NGO recommendedNgo = null;
        try {
            System.out.print("[TEST 5/8] Testing Rule-Based Matching Engine... ");
            List<MatchRecommendation> recs = matchingService.recommendNgosForIssue(newIssue);
            if (!recs.isEmpty()) {
                MatchRecommendation top = recs.get(0);
                recommendedNgo = top.getNgo();
                System.out.println("PASSED!");
                System.out.println("       Top recommendation: " + recommendedNgo.getOrganizationName() + 
                                   " | Score: " + top.getMatchScore() + "% | Reason: " + top.getRecommendationReason());
                passed++;
            } else {
                System.out.println("FAILED! No recommendations produced.");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // 6. Test Transactional Assignment to NGO
        try {
            System.out.print("[TEST 6/8] Testing Multi-Step Transactional Allocation... ");
            boolean allocated = matchingService.allocateIssue(newIssue.getId(), recommendedNgo.getNgoId(), adminUser.getId(), "Allocated based on algorithmic match");
            Issue allocatedIssue = issueService.getIssueById(newIssue.getId());
            if (allocated && allocatedIssue.getStatus() == IssueStatus.ASSIGNED) {
                System.out.println("PASSED! Assigned to NGO #" + recommendedNgo.getNgoId());
                passed++;
            } else {
                System.out.println("FAILED!");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // 7. Test NGO Field Execution (IN_PROGRESS -> RESOLVED)
        try {
            System.out.print("[TEST 7/8] Testing NGO Field Status Progression... ");
            matchingService.updateWorkStatus(newIssue.getId(), recommendedNgo.getNgoId(), recommendedNgo.getId(), 
                    IssueStatus.IN_PROGRESS, "Maintenance team dispatched with pipe fittings");
            matchingService.updateWorkStatus(newIssue.getId(), recommendedNgo.getNgoId(), recommendedNgo.getId(), 
                    IssueStatus.RESOLVED, "Pipeline replaced and water supply restored to all 80 homes");

            Issue resolvedIssue = issueService.getIssueById(newIssue.getId());
            List<StatusHistory> auditTrail = statusHistoryDAO.findByIssueId(newIssue.getId());

            if (resolvedIssue.getStatus() == IssueStatus.RESOLVED && auditTrail.size() >= 4) {
                System.out.println("PASSED! Resolved with " + auditTrail.size() + " audit trail steps.");
                passed++;
            } else {
                System.out.println("FAILED!");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        // 8. Test Analytical Dashboard & Priority Alerts
        try {
            System.out.print("[TEST 8/8] Testing Dashboard Analytics & Priority Alerts... ");
            Report report = reportService.getDashboardReport();
            if (report.getTotalIssues() > 0 && !report.getIssuesByCategory().isEmpty() &&
                report.getNgoPerformanceList() != null) {
                System.out.println("PASSED!");
                System.out.println("       Total Issues: " + report.getTotalIssues() + 
                                   " | Resolved: " + report.getResolvedCount() + 
                                   " | Active Alerts: " + (report.getPriorityAlerts() != null ? report.getPriorityAlerts().size() : 0));
                passed++;
            } else {
                System.out.println("FAILED!");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            failed++;
        }

        System.out.println("\n==============================================================");
        System.out.printf("   VERIFICATION SUMMARY: %d PASSED, %d FAILED\n", passed, failed);
        System.out.println("==============================================================");

        if (failed == 0) {
            System.out.println(">>> ALL PLATFORM SPECIFICATIONS FULLY VERIFIED AND OPERATIONAL! <<<");
        } else {
            System.exit(1);
        }
    }
}

