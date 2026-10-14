package com.ruralhelp.cli;

import com.ruralhelp.dao.StatusHistoryDAO;
import com.ruralhelp.model.*;
import com.ruralhelp.service.*;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive Command Line Interface (CLI) runner.
 */
public class ConsoleApp {
    private final AuthService authService = new AuthService();
    private final IssueService issueService = new IssueService();
    private final MatchingService matchingService = new MatchingService();
    private final NgoService ngoService = new NgoService();
    private final ReportService reportService = new ReportService();
    private final StatusHistoryDAO statusHistoryDAO = new StatusHistoryDAO();
    private final Scanner scanner = new Scanner(System.in);

    public void run() {
        System.out.println("===============================================================");
        System.out.println("            RURAL COMMUNITY SUPPORT PLATFORM (CLI)             ");
        System.out.println("  Connecting rural communities with verified, trackable help   ");
        System.out.println("===============================================================");

        while (true) {
            System.out.println("\nSelect an option:");
            System.out.println("1. Sign In");
            System.out.println("2. Quick Demo Sign In as Admin (admin / admin123)");
            System.out.println("3. Quick Demo Sign In as Jal Seva NGO (jalseva_ngo / ngo123)");
            System.out.println("4. Quick Demo Sign In as Community Member (ramesh_k / user123)");
            System.out.println("5. Exit");
            System.out.print("Choice: ");

            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1":
                        loginPrompt();
                        break;
                    case "2":
                        handleUserSession(authService.login("admin", "admin123"));
                        break;
                    case "3":
                        handleUserSession(authService.login("jalseva_ngo", "ngo123"));
                        break;
                    case "4":
                        handleUserSession(authService.login("ramesh_k", "user123"));
                        break;
                    case "5":
                        System.out.println("Exiting platform. Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private void loginPrompt() {
        System.out.print("Enter username: ");
        String u = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String p = scanner.nextLine().trim();
        try {
            User user = authService.login(u, p);
            if (user == null) {
                System.out.println("Invalid username or password.");
            } else {
                handleUserSession(user);
            }
        } catch (Exception e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private void handleUserSession(User user) {
        if (user == null) return;
        System.out.println("\n>>> Logged in as: " + user.getFullName() + " [" + user.getRole().getDisplayName() + "] <<<");

        switch (user.getRole()) {
            case ADMIN:
                runAdminMenu(user);
                break;
            case NGO:
                if (user instanceof NGO) {
                    runNgoMenu((NGO) user);
                } else {
                    System.out.println("Error loading NGO details.");
                }
                break;
            case COMMUNITY_MEMBER:
                runMemberMenu(user);
                break;
        }
    }

    private void runMemberMenu(User user) {
        while (true) {
            System.out.println("\n--- COMMUNITY MEMBER MENU ---");
            System.out.println("1. Report a New Community Issue");
            System.out.println("2. View My Reported Issues");
            System.out.println("3. View Status History of an Issue");
            System.out.println("4. Logout");
            System.out.print("Choice: ");

            String ch = scanner.nextLine().trim();
            if ("4".equals(ch)) break;
            try {
                switch (ch) {
                    case "1":
                        reportIssuePrompt(user);
                        break;
                    case "2":
                        List<Issue> list = issueService.getIssuesByReporter(user.getId());
                        printIssues(list);
                        break;
                    case "3":
                        viewHistoryPrompt();
                        break;
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private void reportIssuePrompt(User user) throws Exception {
        System.out.println("\n=== Report a Community Issue ===");
        System.out.print("Title: ");
        String title = scanner.nextLine().trim();

        System.out.println("Categories: 1. Clean Water, 2. Sanitation, 3. Healthcare, 4. Education, 5. Waste Management");
        System.out.print("Choose Category (1-5): ");
        int catIdx = Integer.parseInt(scanner.nextLine().trim());
        Category cat = Category.values()[Math.max(0, Math.min(4, catIdx - 1))];

        System.out.print("Village / Area [" + user.getVillageArea() + "]: ");
        String village = scanner.nextLine().trim();
        if (village.isEmpty()) village = user.getVillageArea();

        System.out.println("Urgency: 1. Low, 2. Medium, 3. High, 4. Critical");
        System.out.print("Choose Urgency (1-4): ");
        int urgIdx = Integer.parseInt(scanner.nextLine().trim());
        Urgency urg = Urgency.values()[Math.max(0, Math.min(3, urgIdx - 1))];

        System.out.print("Description: ");
        String desc = scanner.nextLine().trim();

        Issue issue = new Issue();
        issue.setReporterId(user.getId());
        issue.setTitle(title);
        issue.setCategory(cat);
        issue.setVillageArea(village);
        issue.setUrgency(urg);
        issue.setDescription(desc);

        boolean ok = issueService.reportIssue(issue);
        if (ok) {
            System.out.println("Issue reported successfully! Assigned ID: #" + issue.getId() + " (Priority: " + issue.getPriorityScore() + ")");
        }
    }

    private void runNgoMenu(NGO ngo) {
        while (true) {
            System.out.println("\n--- NGO CONSOLE: " + ngo.getOrganizationName() + " ---");
            System.out.println("1. View NGO Profile & Active Capacity");
            System.out.println("2. View Assigned Rural Issues");
            System.out.println("3. Update Issue Work Status (In Progress / Resolved)");
            System.out.println("4. View Issue Audit History");
            System.out.println("5. Logout");
            System.out.print("Choice: ");

            String ch = scanner.nextLine().trim();
            if ("5".equals(ch)) break;
            try {
                switch (ch) {
                    case "1":
                        NGO refreshed = ngoService.getNgoById(ngo.getNgoId());
                        System.out.println("Organization:    " + refreshed.getOrganizationName());
                        System.out.println("Capacity:        " + refreshed.getActiveCases() + " / " + refreshed.getMaxCapacity() + " (" + refreshed.getRemainingCapacity() + " free slots)");
                        System.out.println("Specializations: " + refreshed.getSpecializations());
                        System.out.println("Service Areas:   " + refreshed.getServiceAreas());
                        break;
                    case "2":
                        List<Issue> list = issueService.getIssuesForNgo(ngo.getNgoId());
                        printIssues(list);
                        break;
                    case "3":
                        updateWorkStatusPrompt(ngo);
                        break;
                    case "4":
                        viewHistoryPrompt();
                        break;
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private void updateWorkStatusPrompt(NGO ngo) throws Exception {
        System.out.print("Enter Issue ID: ");
        int issueId = Integer.parseInt(scanner.nextLine().trim());
        System.out.println("1. Set IN_PROGRESS");
        System.out.println("2. Set RESOLVED");
        System.out.print("Choice: ");
        String statusChoice = scanner.nextLine().trim();
        IssueStatus st = "2".equals(statusChoice) ? IssueStatus.RESOLVED : IssueStatus.IN_PROGRESS;
        System.out.print("Progress / Resolution notes: ");
        String notes = scanner.nextLine().trim();

        boolean ok = matchingService.updateWorkStatus(issueId, ngo.getNgoId(), ngo.getId(), st, notes);
        if (ok) {
            System.out.println("Status updated to " + st.getDisplayName());
        }
    }

    private void runAdminMenu(User admin) {
        while (true) {
            System.out.println("\n--- ADMINISTRATOR CONSOLE ---");
            System.out.println("1. Review Pending Verification Queue");
            System.out.println("2. Verify or Reject Issue");
            System.out.println("3. Run NGO Matching Engine & Allocate Issue");
            System.out.println("4. View All Issues Registry");
            System.out.println("5. View Analytics Dashboard & Priority Alerts");
            System.out.println("6. Logout");
            System.out.print("Choice: ");

            String ch = scanner.nextLine().trim();
            if ("6".equals(ch)) break;
            try {
                switch (ch) {
                    case "1":
                        List<Issue> pending = issueService.getPendingIssues();
                        printIssues(pending);
                        break;
                    case "2":
                        System.out.print("Enter Issue ID: ");
                        int issueId = Integer.parseInt(scanner.nextLine().trim());
                        System.out.print("Action (1 = Verify, 2 = Reject): ");
                        String act = scanner.nextLine().trim();
                        System.out.print("Notes / Reason: ");
                        String notes = scanner.nextLine().trim();
                        if ("1".equals(act)) {
                            issueService.verifyIssue(issueId, admin.getId(), notes);
                            System.out.println("Issue verified!");
                        } else {
                            issueService.rejectIssue(issueId, admin.getId(), notes);
                            System.out.println("Issue rejected.");
                        }
                        break;
                    case "3":
                        runMatchingPrompt(admin);
                        break;
                    case "4":
                        printIssues(issueService.getAllIssues());
                        break;
                    case "5":
                        showDashboardCli();
                        break;
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private void runMatchingPrompt(User admin) throws Exception {
        List<Issue> verified = issueService.getVerifiedIssues();
        if (verified.isEmpty()) {
            System.out.println("No verified issues awaiting allocation.");
            return;
        }
        System.out.println("\nVerified Issues Awaiting NGO Allocation:");
        printIssues(verified);

        System.out.print("Enter Issue ID to run Matching Engine: ");
        int issueId = Integer.parseInt(scanner.nextLine().trim());
        Issue issue = issueService.getIssueById(issueId);
        if (issue == null) {
            System.out.println("Issue not found.");
            return;
        }

        List<MatchRecommendation> recs = matchingService.recommendNgosForIssue(issue);
        System.out.println("\n--- Matching Engine Recommendations for Issue #" + issueId + " ---");
        for (int i = 0; i < recs.size(); i++) {
            MatchRecommendation r = recs.get(i);
            System.out.printf("[%d] Score: %3d%% | NGO: %-25s | Free Slots: %d | Factors: %s\n",
                    (i + 1), r.getMatchScore(), r.getNgo().getOrganizationName(), 
                    r.getNgo().getRemainingCapacity(), r.getRecommendationReason());
        }

        System.out.print("Select recommendation number to allocate (or 0 to cancel): ");
        int sel = Integer.parseInt(scanner.nextLine().trim());
        if (sel >= 1 && sel <= recs.size()) {
            MatchRecommendation chosen = recs.get(sel - 1);
            System.out.print("Allocation notes: ");
            String notes = scanner.nextLine().trim();
            matchingService.allocateIssue(issueId, chosen.getNgo().getNgoId(), admin.getId(), notes);
            System.out.println("Successfully allocated Issue #" + issueId + " to " + chosen.getNgo().getOrganizationName() + "!");
        }
    }

    private void showDashboardCli() throws Exception {
        Report r = reportService.getDashboardReport();
        System.out.println("\n================ PLATFORM DASHBOARD ================");
        System.out.printf("Total Issues: %d | Unresolved: %d | In Progress: %d | Resolved: %d\n",
                r.getTotalIssues(), r.getUnresolvedCount(), r.getInProgressCount(), r.getResolvedCount());
        System.out.println("\n--- Category Breakdown ---");
        for (Map.Entry<String, Integer> e : r.getIssuesByCategory().entrySet()) {
            System.out.printf("  %-25s: %d\n", e.getKey(), e.getValue());
        }
        System.out.println("\n--- Area Breakdown ---");
        for (Map.Entry<String, Integer> e : r.getIssuesByArea().entrySet()) {
            System.out.printf("  %-25s: %d\n", e.getKey(), e.getValue());
        }
        System.out.println("\n--- Priority Alerts & Hotspots ---");
        for (Report.PriorityAlert pa : r.getPriorityAlerts()) {
            System.out.printf("  [%s] %s (%s) - %s\n", pa.getAlertLevel(), pa.getVillageArea(), pa.getCategory(), pa.getReason());
        }
    }

    private void viewHistoryPrompt() throws Exception {
        System.out.print("Enter Issue ID: ");
        int issueId = Integer.parseInt(scanner.nextLine().trim());
        List<StatusHistory> history = statusHistoryDAO.findByIssueId(issueId);
        System.out.println("\n=== History for Issue #" + issueId + " ===");
        for (StatusHistory sh : history) {
            System.out.printf("• [%s] %s (by %s) - %s\n", 
                    com.ruralhelp.util.DateTimeUtil.format(sh.getChangedAt()),
                    sh.getNewStatus(), sh.getChangedByName(), 
                    sh.getComments() != null ? sh.getComments() : "");
        }
    }

    private void printIssues(List<Issue> list) {
        if (list.isEmpty()) {
            System.out.println("No issues found.");
            return;
        }
        System.out.printf("%-4s %-32s %-15s %-12s %-10s %-18s %-5s\n",
                "ID", "TITLE", "CATEGORY", "VILLAGE", "URGENCY", "STATUS", "SCORE");
        System.out.println("--------------------------------------------------------------------------------------------------");
        for (Issue i : list) {
            String title = i.getTitle().length() > 30 ? i.getTitle().substring(0, 27) + "..." : i.getTitle();
            System.out.printf("%-4d %-32s %-15s %-12s %-10s %-18s %-5d\n",
                    i.getId(), title, i.getCategory().getDisplayName(),
                    i.getVillageArea(), i.getUrgency().getDisplayName(),
                    i.getStatus().getDisplayName(), i.getPriorityScore());
        }
    }
}

