package com.ruralhelp.ui;

import com.ruralhelp.dao.StatusHistoryDAO;
import com.ruralhelp.model.*;
import com.ruralhelp.service.IssueService;
import com.ruralhelp.service.MatchingService;
import com.ruralhelp.service.NgoService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * NGO stakeholder dashboard:
 * 1. View organization specializations, service areas, and active capacity
 * 2. Manage assigned issues and update work status through completion
 */
public class NgoPanel extends JPanel {
    private final NGO currentNgo;
    private final IssueService issueService = new IssueService();
    private final NgoService ngoService = new NgoService();
    private final MatchingService matchingService = new MatchingService();
    private final StatusHistoryDAO statusHistoryDAO = new StatusHistoryDAO();

    private final JLabel capacityLabel = new JLabel();
    private final JLabel specsLabel = new JLabel();
    private final JLabel areasLabel = new JLabel();

    private final DefaultTableModel tableModel;
    private final JTable tasksTable;

    public NgoPanel(NGO currentNgo) {
        this.currentNgo = currentNgo;
        this.setLayout(new BorderLayout(12, 12));
        this.setBorder(new EmptyBorder(12, 16, 12, 16));

        String[] cols = {"Issue ID", "Title", "Category", "Village", "Urgency", "Status", "Reporter", "Contact"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tasksTable = new JTable(tableModel);
        UITheme.styleTable(tasksTable);

        initUI();
        loadProfileData();
        loadAssignedTasks();
    }

    private void initUI() {
        // Top: Profile and Capacity Summary Card
        JPanel profileCard = UITheme.createCardPanel();
        profileCard.setLayout(new GridLayout(2, 2, 12, 8));

        JLabel nameLbl = new JLabel(currentNgo.getOrganizationName() + " (" + currentNgo.getContactPerson() + ")");
        nameLbl.setFont(UITheme.FONT_TITLE);
        nameLbl.setForeground(UITheme.PRIMARY_DARK);

        capacityLabel.setFont(UITheme.FONT_BODY_BOLD);
        specsLabel.setFont(UITheme.FONT_SMALL);
        areasLabel.setFont(UITheme.FONT_SMALL);

        profileCard.add(nameLbl);
        profileCard.add(capacityLabel);
        profileCard.add(specsLabel);
        profileCard.add(areasLabel);

        add(profileCard, BorderLayout.NORTH);

        // Center: Tasks Table Card
        JPanel tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(8, 8));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        JLabel title = new JLabel("Assigned Rural Issues & Work Execution");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.PRIMARY_DARK);
        headerPanel.add(title, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnPanel.setBackground(Color.WHITE);
        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        JButton inProgBtn = UITheme.createSecondaryButton("Start Work (In Progress)");
        JButton resolveBtn = UITheme.createPrimaryButton("Mark Resolved");
        JButton historyBtn = UITheme.createSecondaryButton("View History");

        refreshBtn.addActionListener(e -> { loadProfileData(); loadAssignedTasks(); });
        inProgBtn.addActionListener(e -> updateStatus(IssueStatus.IN_PROGRESS));
        resolveBtn.addActionListener(e -> updateStatus(IssueStatus.RESOLVED));
        historyBtn.addActionListener(e -> showHistory());

        btnPanel.add(refreshBtn);
        btnPanel.add(inProgBtn);
        btnPanel.add(resolveBtn);
        btnPanel.add(historyBtn);
        headerPanel.add(btnPanel, BorderLayout.EAST);

        tableCard.add(headerPanel, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(tasksTable), BorderLayout.CENTER);

        add(tableCard, BorderLayout.CENTER);
    }

    private void loadProfileData() {
        try {
            NGO refreshed = ngoService.getNgoById(currentNgo.getNgoId());
            if (refreshed != null) {
                currentNgo.setActiveCases(refreshed.getActiveCases());
                currentNgo.setCompletedCases(refreshed.getCompletedCases());
                currentNgo.setSpecializations(refreshed.getSpecializations());
                currentNgo.setServiceAreas(refreshed.getServiceAreas());
            }

            capacityLabel.setText("Active Caseload: " + currentNgo.getActiveCases() + " / " + 
                    currentNgo.getMaxCapacity() + " (" + currentNgo.getRemainingCapacity() + " available slots)");
            if (!currentNgo.hasAvailableCapacity()) {
                capacityLabel.setForeground(UITheme.COLOR_CRITICAL);
            } else {
                capacityLabel.setForeground(UITheme.COLOR_LOW);
            }

            specsLabel.setText("Specializations: " + currentNgo.getSpecializations());
            areasLabel.setText("Service Areas: " + currentNgo.getServiceAreas());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadAssignedTasks() {
        tableModel.setRowCount(0);
        try {
            List<Issue> list = issueService.getIssuesForNgo(currentNgo.getNgoId());
            for (Issue i : list) {
                tableModel.addRow(new Object[]{
                        i.getId(),
                        i.getTitle(),
                        i.getCategory().getDisplayName(),
                        i.getVillageArea(),
                        i.getUrgency().getDisplayName(),
                        i.getStatus().getDisplayName(),
                        i.getReporterName(),
                        i.getReporterPhone()
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateStatus(IssueStatus newStatus) {
        int row = tasksTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an assigned issue from the table first.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int issueId = (Integer) tableModel.getValueAt(row, 0);
        String actionPrompt = newStatus == IssueStatus.RESOLVED 
                ? "Enter resolution details (action taken, verified outcome):" 
                : "Enter progress notes for starting work:";

        String notes = JOptionPane.showInputDialog(this, actionPrompt, "Update Work Status", JOptionPane.QUESTION_MESSAGE);
        if (notes == null) return; // User cancelled

        try {
            boolean ok = matchingService.updateWorkStatus(issueId, currentNgo.getNgoId(), currentNgo.getId(), newStatus, notes.trim());
            if (ok) {
                JOptionPane.showMessageDialog(this, "Status updated to " + newStatus.getDisplayName(), "Success", JOptionPane.INFORMATION_MESSAGE);
                loadProfileData();
                loadAssignedTasks();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to update status: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showHistory() {
        int row = tasksTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an issue first.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int issueId = (Integer) tableModel.getValueAt(row, 0);
        try {
            List<StatusHistory> history = statusHistoryDAO.findByIssueId(issueId);
            StringBuilder sb = new StringBuilder();
            sb.append("=== History for Issue #").append(issueId).append(" ===\n\n");
            for (StatusHistory sh : history) {
                sb.append("• [").append(com.ruralhelp.util.DateTimeUtil.format(sh.getChangedAt())).append("] ")
                  .append(sh.getNewStatus())
                  .append(" by ").append(sh.getChangedByName())
                  .append("\n  Notes: ").append(sh.getComments() != null ? sh.getComments() : "None")
                  .append("\n\n");
            }

            JTextArea area = new JTextArea(sb.toString(), 14, 45);
            area.setEditable(false);
            area.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Work-Status Timeline", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

