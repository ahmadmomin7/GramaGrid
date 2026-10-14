package com.ruralhelp.ui;

import com.ruralhelp.dao.StatusHistoryDAO;
import com.ruralhelp.model.*;
import com.ruralhelp.service.IssueService;
import com.ruralhelp.service.MatchingService;
import com.ruralhelp.service.NgoService;
import com.ruralhelp.service.ReportService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Administrative coordinator console implementing:
 * 1. Verification queue
 * 2. Rule-based NGO matching & allocation engine
 * 3. Master tracking and issue lifecycle supervision
 * 4. NGO registration and management
 * 5. Analytical dashboards & priority prediction alerts
 */
public class AdminPanel extends JPanel {
    private final User currentUser;
    private final IssueService issueService = new IssueService();
    private final MatchingService matchingService = new MatchingService();
    private final NgoService ngoService = new NgoService();
    private final ReportService reportService = new ReportService();
    private final StatusHistoryDAO statusHistoryDAO = new StatusHistoryDAO();

    private final JTabbedPane tabbedPane = new JTabbedPane();

    // 1. Verification Queue
    private DefaultTableModel verifyModel;
    private JTable verifyTable;

    // 2. Matching & Allocation
    private DefaultTableModel verifiedIssuesModel;
    private JTable verifiedIssuesTable;
    private DefaultTableModel matchModel;
    private JTable matchTable;
    private List<MatchRecommendation> currentRecommendations;
    private Issue selectedVerifiedIssue;

    // 3. All Issues
    private DefaultTableModel allIssuesModel;
    private JTable allIssuesTable;

    // 4. NGO Management
    private DefaultTableModel ngoModel;
    private JTable ngoTable;

    // 5. Dashboard components
    private JPanel metricsPanel;
    private JTextArea categoryStatsArea;
    private JTextArea areaStatsArea;
    private DefaultTableModel ngoPerfModel;
    private JTable ngoPerfTable;
    private JTextArea alertsArea;

    public AdminPanel(User currentUser) {
        this.currentUser = currentUser;
        this.setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        tabbedPane.setFont(UITheme.FONT_TITLE);
        tabbedPane.addTab("1. Verification Queue", createVerificationPanel());
        tabbedPane.addTab("2. Matching & Allocation", createMatchingPanel());
        tabbedPane.addTab("3. All Issues & Tracking", createAllIssuesPanel());
        tabbedPane.addTab("4. NGO Management", createNgoManagementPanel());
        tabbedPane.addTab("5. Dashboards & Analytics", createDashboardPanel());

        tabbedPane.addChangeListener(e -> refreshCurrentTab());

        add(tabbedPane, BorderLayout.CENTER);
        refreshCurrentTab();
    }

    private void refreshCurrentTab() {
        int index = tabbedPane.getSelectedIndex();
        switch (index) {
            case 0: loadVerificationQueue(); break;
            case 1: loadVerifiedIssuesForMatching(); break;
            case 2: loadAllIssues(); break;
            case 3: loadNgos(); break;
            case 4: loadDashboard(); break;
        }
    }

    // =========================================================================
    // Tab 1: Verification Queue
    // =========================================================================
    private JPanel createVerificationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Pending Verification Queue (New Reports)");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.PRIMARY_DARK);
        top.add(title, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        JButton verifyBtn = UITheme.createPrimaryButton("Verify Issue");
        JButton rejectBtn = UITheme.createDangerButton("Reject Issue");
        JButton detailsBtn = UITheme.createSecondaryButton("View Details");

        refreshBtn.addActionListener(e -> loadVerificationQueue());
        verifyBtn.addActionListener(e -> verifySelectedIssue());
        rejectBtn.addActionListener(e -> rejectSelectedIssue());
        detailsBtn.addActionListener(e -> showDetails(verifyTable, verifyModel));

        btns.add(refreshBtn);
        btns.add(detailsBtn);
        btns.add(rejectBtn);
        btns.add(verifyBtn);
        top.add(btns, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        String[] cols = {"ID", "Title", "Category", "Village", "Urgency", "Priority Score", "Reporter", "Reported"};
        verifyModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        verifyTable = new JTable(verifyModel);
        UITheme.styleTable(verifyTable);
        panel.add(new JScrollPane(verifyTable), BorderLayout.CENTER);

        return panel;
    }

    private void loadVerificationQueue() {
        verifyModel.setRowCount(0);
        try {
            List<Issue> list = issueService.getPendingIssues();
            for (Issue i : list) {
                verifyModel.addRow(new Object[]{
                        i.getId(),
                        i.getTitle(),
                        i.getCategory().getDisplayName(),
                        i.getVillageArea(),
                        i.getUrgency().getDisplayName(),
                        i.getPriorityScore(),
                        i.getReporterName(),
                        com.ruralhelp.util.DateTimeUtil.formatRelative(i.getCreatedAt())
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void verifySelectedIssue() {
        int row = verifyTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an issue to verify.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int issueId = (Integer) verifyModel.getValueAt(row, 0);
        String notes = JOptionPane.showInputDialog(this, "Verification notes (e.g. cross-verified with village leader):", "Verify Issue #" + issueId, JOptionPane.QUESTION_MESSAGE);
        if (notes == null) return;

        try {
            issueService.verifyIssue(issueId, currentUser.getId(), notes);
            JOptionPane.showMessageDialog(this, "Issue #" + issueId + " verified! It is now available in Matching & Allocation.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadVerificationQueue();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rejectSelectedIssue() {
        int row = verifyTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an issue to reject.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int issueId = (Integer) verifyModel.getValueAt(row, 0);
        String reason = JOptionPane.showInputDialog(this, "Enter rejection reason:", "Reject Issue #" + issueId, JOptionPane.WARNING_MESSAGE);
        if (reason == null || reason.trim().isEmpty()) return;

        try {
            issueService.rejectIssue(issueId, currentUser.getId(), reason);
            JOptionPane.showMessageDialog(this, "Issue #" + issueId + " rejected.", "Rejected", JOptionPane.INFORMATION_MESSAGE);
            loadVerificationQueue();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // Tab 2: Matching & Allocation Engine
    // =========================================================================
    private JPanel createMatchingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Intelligent NGO Matching & Allocation Engine (Need + Place + Capacity)");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.PRIMARY_DARK);
        top.add(title, BorderLayout.WEST);

        JButton allocBtn = UITheme.createPrimaryButton("Allocate Selected Issue to Recommended NGO");
        allocBtn.addActionListener(e -> allocateIssue());
        top.add(allocBtn, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.45);

        // Left: Verified issues table
        JPanel leftCard = UITheme.createCardPanel();
        leftCard.setLayout(new BorderLayout(6, 6));
        leftCard.add(new JLabel("1. Select Verified Issue:"), BorderLayout.NORTH);

        String[] vCols = {"ID", "Title", "Category", "Village", "Urgency", "Priority"};
        verifiedIssuesModel = new DefaultTableModel(vCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        verifiedIssuesTable = new JTable(verifiedIssuesModel);
        UITheme.styleTable(verifiedIssuesTable);

        verifiedIssuesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onVerifiedIssueSelected();
            }
        });

        leftCard.add(new JScrollPane(verifiedIssuesTable), BorderLayout.CENTER);
        splitPane.setLeftComponent(leftCard);

        // Right: Recommendations table
        JPanel rightCard = UITheme.createCardPanel();
        rightCard.setLayout(new BorderLayout(6, 6));
        rightCard.add(new JLabel("2. Recommended NGOs (Ranked by Match Score):"), BorderLayout.NORTH);

        String[] mCols = {"Score", "NGO Organization", "Need Fit", "Place Fit", "Capacity", "Recommendation Factor"};
        matchModel = new DefaultTableModel(mCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        matchTable = new JTable(matchModel);
        UITheme.styleTable(matchTable);
        rightCard.add(new JScrollPane(matchTable), BorderLayout.CENTER);
        splitPane.setRightComponent(rightCard);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadVerifiedIssuesForMatching() {
        verifiedIssuesModel.setRowCount(0);
        matchModel.setRowCount(0);
        selectedVerifiedIssue = null;
        try {
            List<Issue> list = issueService.getVerifiedIssues();
            for (Issue i : list) {
                verifiedIssuesModel.addRow(new Object[]{
                        i.getId(),
                        i.getTitle(),
                        i.getCategory().getDisplayName(),
                        i.getVillageArea(),
                        i.getUrgency().getDisplayName(),
                        i.getPriorityScore()
                });
            }
            if (verifiedIssuesModel.getRowCount() > 0) {
                verifiedIssuesTable.setRowSelectionInterval(0, 0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onVerifiedIssueSelected() {
        int row = verifiedIssuesTable.getSelectedRow();
        matchModel.setRowCount(0);
        if (row < 0) {
            selectedVerifiedIssue = null;
            return;
        }

        int issueId = (Integer) verifiedIssuesModel.getValueAt(row, 0);
        try {
            selectedVerifiedIssue = issueService.getIssueById(issueId);
            if (selectedVerifiedIssue != null) {
                currentRecommendations = matchingService.recommendNgosForIssue(selectedVerifiedIssue);
                for (MatchRecommendation rec : currentRecommendations) {
                    NGO ngo = rec.getNgo();
                    matchModel.addRow(new Object[]{
                            rec.getMatchScore() + "%",
                            ngo.getOrganizationName(),
                            rec.isCategoryMatched() ? "YES" : "NO",
                            rec.isAreaMatched() ? "YES" : "NO",
                            ngo.getRemainingCapacity() + " / " + ngo.getMaxCapacity(),
                            rec.getRecommendationReason()
                    });
                }
                if (matchModel.getRowCount() > 0) {
                    matchTable.setRowSelectionInterval(0, 0);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void allocateIssue() {
        if (selectedVerifiedIssue == null) {
            JOptionPane.showMessageDialog(this, "Please select an issue to allocate.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int matchRow = matchTable.getSelectedRow();
        if (matchRow < 0 || currentRecommendations == null || matchRow >= currentRecommendations.size()) {
            JOptionPane.showMessageDialog(this, "Please select an NGO from the recommendations list.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        MatchRecommendation rec = currentRecommendations.get(matchRow);
        NGO ngo = rec.getNgo();

        String notes = JOptionPane.showInputDialog(this, 
                "Confirm assignment of Issue #" + selectedVerifiedIssue.getId() + " to:\n" 
                + ngo.getOrganizationName() + " (Score: " + rec.getMatchScore() + "%)\n\nAllocation instructions/notes:",
                "Confirm NGO Allocation", JOptionPane.QUESTION_MESSAGE);
        if (notes == null) return;

        try {
            boolean ok = matchingService.allocateIssue(selectedVerifiedIssue.getId(), ngo.getNgoId(), currentUser.getId(), notes);
            if (ok) {
                JOptionPane.showMessageDialog(this, 
                        "Successfully allocated Issue #" + selectedVerifiedIssue.getId() + " to " + ngo.getOrganizationName() + "!",
                        "Allocation Complete", JOptionPane.INFORMATION_MESSAGE);
                loadVerifiedIssuesForMatching();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Allocation failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // Tab 3: All Issues & Tracking
    // =========================================================================
    private JPanel createAllIssuesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Master Registry & Lifecycle Tracker");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.PRIMARY_DARK);
        top.add(title, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        JButton detailsBtn = UITheme.createSecondaryButton("View Full History");
        JButton closeBtn = UITheme.createPrimaryButton("Close Issue");

        refreshBtn.addActionListener(e -> loadAllIssues());
        detailsBtn.addActionListener(e -> showDetails(allIssuesTable, allIssuesModel));
        closeBtn.addActionListener(e -> closeResolvedIssue());

        btns.add(refreshBtn);
        btns.add(detailsBtn);
        btns.add(closeBtn);
        top.add(btns, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        String[] cols = {"ID", "Title", "Category", "Village", "Urgency", "Status", "Priority", "Assigned NGO", "Updated"};
        allIssuesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        allIssuesTable = new JTable(allIssuesModel);
        UITheme.styleTable(allIssuesTable);
        panel.add(new JScrollPane(allIssuesTable), BorderLayout.CENTER);

        return panel;
    }

    private void loadAllIssues() {
        allIssuesModel.setRowCount(0);
        try {
            List<Issue> list = issueService.getAllIssues();
            for (Issue i : list) {
                allIssuesModel.addRow(new Object[]{
                        i.getId(),
                        i.getTitle(),
                        i.getCategory().getDisplayName(),
                        i.getVillageArea(),
                        i.getUrgency().getDisplayName(),
                        i.getStatus().getDisplayName(),
                        i.getPriorityScore(),
                        i.getAssignedNgoName() != null ? i.getAssignedNgoName() : "None",
                        com.ruralhelp.util.DateTimeUtil.formatRelative(i.getUpdatedAt())
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void closeResolvedIssue() {
        int row = allIssuesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an issue to close.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int issueId = (Integer) allIssuesModel.getValueAt(row, 0);
        String status = (String) allIssuesModel.getValueAt(row, 5);

        if (!status.equalsIgnoreCase(IssueStatus.RESOLVED.getDisplayName())) {
            int proceed = JOptionPane.showConfirmDialog(this, 
                    "Issue #" + issueId + " is currently in status: " + status + ".\nAre you sure you want to officially CLOSE it?",
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            if (proceed != JOptionPane.YES_OPTION) return;
        }

        String notes = JOptionPane.showInputDialog(this, "Enter closing confirmation notes:", "Close Issue #" + issueId, JOptionPane.QUESTION_MESSAGE);
        if (notes == null) return;

        try {
            issueService.closeIssue(issueId, currentUser.getId(), notes);
            JOptionPane.showMessageDialog(this, "Issue #" + issueId + " closed.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadAllIssues();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // Tab 4: NGO Management
    // =========================================================================
    private JPanel createNgoManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Registered NGOs, Specializations, and Capacity Limits");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.PRIMARY_DARK);
        top.add(title, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        JButton regBtn = UITheme.createPrimaryButton("Register New NGO");

        refreshBtn.addActionListener(e -> loadNgos());
        regBtn.addActionListener(e -> {
            RegisterDialog dlg = new RegisterDialog((Frame) SwingUtilities.getWindowAncestor(this), new com.ruralhelp.service.AuthService());
            dlg.setVisible(true);
            if (dlg.isRegistered()) loadNgos();
        });

        btns.add(refreshBtn);
        btns.add(regBtn);
        top.add(btns, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        String[] cols = {"ID", "Organization", "Contact Person", "Phone", "Email", "Capacity (Active / Max)", "Specializations", "Service Villages"};
        ngoModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ngoTable = new JTable(ngoModel);
        UITheme.styleTable(ngoTable);
        panel.add(new JScrollPane(ngoTable), BorderLayout.CENTER);

        return panel;
    }

    private void loadNgos() {
        ngoModel.setRowCount(0);
        try {
            List<NGO> list = ngoService.getAllNgos();
            for (NGO n : list) {
                ngoModel.addRow(new Object[]{
                        n.getNgoId(),
                        n.getOrganizationName(),
                        n.getContactPerson(),
                        n.getPhone(),
                        n.getEmail(),
                        n.getActiveCases() + " / " + n.getMaxCapacity(),
                        n.getSpecializations().toString(),
                        n.getServiceAreas().toString()
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // Tab 5: Dashboards & Analytics
    // =========================================================================
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Platform Analytics & Rule-Based Priority Prediction");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.PRIMARY_DARK);
        top.add(title, BorderLayout.WEST);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh Analytics");
        refreshBtn.addActionListener(e -> loadDashboard());
        top.add(refreshBtn, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        // Metric cards container
        metricsPanel = new JPanel(new GridLayout(1, 5, 10, 10));
        panel.add(metricsPanel, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(2, 2, 10, 10));

        // Box 1: Category breakdown
        JPanel catCard = UITheme.createCardPanel();
        catCard.setLayout(new BorderLayout(4, 4));
        catCard.add(new JLabel("Common Issues by Category (Slide 9):"), BorderLayout.NORTH);
        categoryStatsArea = new JTextArea();
        categoryStatsArea.setEditable(false);
        categoryStatsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        catCard.add(new JScrollPane(categoryStatsArea), BorderLayout.CENTER);
        content.add(catCard);

        // Box 2: Area breakdown
        JPanel areaCard = UITheme.createCardPanel();
        areaCard.setLayout(new BorderLayout(4, 4));
        areaCard.add(new JLabel("Common Issues by Village / Sector:"), BorderLayout.NORTH);
        areaStatsArea = new JTextArea();
        areaStatsArea.setEditable(false);
        areaStatsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaCard.add(new JScrollPane(areaStatsArea), BorderLayout.CENTER);
        content.add(areaCard);

        // Box 3: NGO Performance
        JPanel perfCard = UITheme.createCardPanel();
        perfCard.setLayout(new BorderLayout(4, 4));
        perfCard.add(new JLabel("NGO Response & Completion Performance:"), BorderLayout.NORTH);
        String[] perfCols = {"NGO Organization", "Active", "Completed", "Resolution Rate"};
        ngoPerfModel = new DefaultTableModel(perfCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ngoPerfTable = new JTable(ngoPerfModel);
        UITheme.styleTable(ngoPerfTable);
        perfCard.add(new JScrollPane(ngoPerfTable), BorderLayout.CENTER);
        content.add(perfCard);

        // Box 4: Priority Alerts
        JPanel alertsCard = UITheme.createCardPanel();
        alertsCard.setLayout(new BorderLayout(4, 4));
        JLabel alertTitle = new JLabel("Priority Alerts (Hotspots & Recurrence Flags):");
        alertTitle.setForeground(UITheme.COLOR_CRITICAL);
        alertsCard.add(alertTitle, BorderLayout.NORTH);
        alertsArea = new JTextArea();
        alertsArea.setEditable(false);
        alertsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        alertsCard.add(new JScrollPane(alertsArea), BorderLayout.CENTER);
        content.add(alertsCard);

        JPanel centerWrap = new JPanel(new BorderLayout(8, 8));
        centerWrap.add(metricsPanel, BorderLayout.NORTH);
        centerWrap.add(content, BorderLayout.CENTER);

        panel.add(centerWrap, BorderLayout.CENTER);
        return panel;
    }

    private void loadDashboard() {
        try {
            Report report = reportService.getDashboardReport();

            // Refresh metric cards
            metricsPanel.removeAll();
            metricsPanel.add(UITheme.createMetricCard("Total Issues", String.valueOf(report.getTotalIssues()), UITheme.PRIMARY));
            metricsPanel.add(UITheme.createMetricCard("Unresolved", String.valueOf(report.getUnresolvedCount()), UITheme.COLOR_HIGH));
            metricsPanel.add(UITheme.createMetricCard("In Progress", String.valueOf(report.getInProgressCount()), UITheme.ACCENT_TEAL));
            metricsPanel.add(UITheme.createMetricCard("Resolved", String.valueOf(report.getResolvedCount()), UITheme.COLOR_RESOLVED));
            metricsPanel.add(UITheme.createMetricCard("Active NGOs", report.getActiveNgos() + " / " + report.getTotalNgos(), UITheme.PRIMARY_DARK));
            metricsPanel.revalidate();
            metricsPanel.repaint();

            // Category breakdown
            StringBuilder catSb = new StringBuilder();
            catSb.append(String.format("%-25s %s\n", "CATEGORY", "REPORTED COUNT"));
            catSb.append("------------------------------------------\n");
            for (Map.Entry<String, Integer> e : report.getIssuesByCategory().entrySet()) {
                catSb.append(String.format("%-25s %d\n", e.getKey(), e.getValue()));
            }
            categoryStatsArea.setText(catSb.toString());

            // Area breakdown
            StringBuilder areaSb = new StringBuilder();
            areaSb.append(String.format("%-25s %s\n", "VILLAGE / SECTOR", "REPORTED COUNT"));
            areaSb.append("------------------------------------------\n");
            for (Map.Entry<String, Integer> e : report.getIssuesByArea().entrySet()) {
                areaSb.append(String.format("%-25s %d\n", e.getKey(), e.getValue()));
            }
            areaStatsArea.setText(areaSb.toString());

            // NGO Performance
            ngoPerfModel.setRowCount(0);
            if (report.getNgoPerformanceList() != null) {
                for (Report.NgoPerformanceRecord rec : report.getNgoPerformanceList()) {
                    ngoPerfModel.addRow(new Object[]{
                            rec.getNgoName(),
                            rec.getActiveCases(),
                            rec.getCompletedCases(),
                            String.format("%.1f%%", rec.getResolutionRate())
                    });
                }
            }

            // Priority alerts
            StringBuilder alertSb = new StringBuilder();
            if (report.getPriorityAlerts() != null && !report.getPriorityAlerts().isEmpty()) {
                for (Report.PriorityAlert pa : report.getPriorityAlerts()) {
                    alertSb.append("[").append(pa.getAlertLevel()).append("] ")
                           .append(pa.getVillageArea()).append(" - ").append(pa.getCategory())
                           .append(" (").append(pa.getComplaintCount()).append(" active issues)\n")
                           .append("  Reason: ").append(pa.getReason()).append("\n\n");
                }
            } else {
                alertSb.append("No critical hotspots detected. All regions operating within normal limits.\n");
            }
            alertsArea.setText(alertSb.toString());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void showDetails(JTable table, DefaultTableModel model) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an issue first.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int issueId = (Integer) model.getValueAt(row, 0);
        try {
            Issue issue = issueService.getIssueById(issueId);
            List<StatusHistory> history = statusHistoryDAO.findByIssueId(issueId);

            StringBuilder sb = new StringBuilder();
            sb.append("=== ISSUE DETAILS #").append(issue.getId()).append(" ===\n");
            sb.append("Title:       ").append(issue.getTitle()).append("\n");
            sb.append("Category:    ").append(issue.getCategory().getDisplayName()).append("\n");
            sb.append("Village:     ").append(issue.getVillageArea()).append("\n");
            sb.append("Urgency:     ").append(issue.getUrgency().getDisplayName()).append("\n");
            sb.append("Status:      ").append(issue.getStatus().getDisplayName()).append("\n");
            sb.append("Priority:    ").append(issue.getPriorityScore()).append(" / 100\n");
            sb.append("Reporter:    ").append(issue.getReporterName()).append(" (").append(issue.getReporterPhone()).append(")\n");
            sb.append("Assigned To: ").append(issue.getAssignedNgoName() != null ? issue.getAssignedNgoName() : "None").append("\n");
            sb.append("Photo Path:  ").append(issue.getPhotoPath() != null ? issue.getPhotoPath() : "None").append("\n\n");
            sb.append("Description:\n").append(issue.getDescription()).append("\n\n");
            sb.append("--- WORK-STATUS TIMELINE ---\n");
            for (StatusHistory sh : history) {
                sb.append("• [").append(com.ruralhelp.util.DateTimeUtil.format(sh.getChangedAt())).append("] ")
                  .append(sh.getNewStatus()).append(" (by ").append(sh.getChangedByName()).append(")\n")
                  .append("  Notes: ").append(sh.getComments() != null ? sh.getComments() : "").append("\n");
            }

            JTextArea area = new JTextArea(sb.toString(), 18, 50);
            area.setEditable(false);
            area.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Issue Record & Audit Trail", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
