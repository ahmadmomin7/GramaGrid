package com.ruralhelp.ui;

import com.ruralhelp.dao.StatusHistoryDAO;
import com.ruralhelp.model.*;
import com.ruralhelp.service.IssueService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Community Member view:
 * 1. Report new issues (Category, Area, Urgency, Description, Photo)
 * 2. Track transparent status workflow of submitted issues
 */
public class CommunityMemberPanel extends JPanel {
    private final User currentUser;
    private final IssueService issueService = new IssueService();
    private final StatusHistoryDAO statusHistoryDAO = new StatusHistoryDAO();

    // Form inputs
    private final JTextField titleField = new JTextField(20);
    private final JComboBox<Category> categoryCombo = new JComboBox<>(Category.values());
    private final JTextField villageField = new JTextField(15);
    private final JComboBox<Urgency> urgencyCombo = new JComboBox<>(Urgency.values());
    private final JTextArea descArea = new JTextArea(4, 20);
    private final JTextField photoField = new JTextField(15);
    private final JLabel priorityBadge = new JLabel("Estimated Priority: Medium (50)");

    // Table
    private final DefaultTableModel tableModel;
    private final JTable issuesTable;

    public CommunityMemberPanel(User currentUser) {
        this.currentUser = currentUser;
        this.setLayout(new BorderLayout(12, 12));
        this.setBorder(new EmptyBorder(12, 16, 12, 16));

        String[] cols = {"ID", "Title", "Category", "Village", "Urgency", "Status", "Priority", "Reported"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        issuesTable = new JTable(tableModel);
        UITheme.styleTable(issuesTable);

        initUI();
        loadIssues();
    }

    private void initUI() {
        // Left: Report Form Card
        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new BorderLayout(8, 8));
        formCard.setPreferredSize(new Dimension(380, 0));

        JLabel formHeader = new JLabel("Report a Community Issue");
        formHeader.setFont(UITheme.FONT_TITLE);
        formHeader.setForeground(UITheme.PRIMARY_DARK);
        formCard.add(formHeader, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; fieldsPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1; fieldsPanel.add(titleField, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; fieldsPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; fieldsPanel.add(categoryCombo, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; fieldsPanel.add(new JLabel("Village/Area:"), gbc);
        villageField.setText(currentUser.getVillageArea() != null ? currentUser.getVillageArea() : "");
        gbc.gridx = 1; fieldsPanel.add(villageField, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; fieldsPanel.add(new JLabel("Urgency:"), gbc);
        urgencyCombo.setSelectedItem(Urgency.MEDIUM);
        gbc.gridx = 1; fieldsPanel.add(urgencyCombo, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; fieldsPanel.add(new JLabel("Description:"), gbc);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        gbc.gridx = 1; fieldsPanel.add(descScroll, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; fieldsPanel.add(new JLabel("Photo (optional):"), gbc);
        JPanel photoRow = new JPanel(new BorderLayout(4, 0));
        photoRow.setBackground(Color.WHITE);
        photoRow.add(photoField, BorderLayout.CENTER);
        JButton browseBtn = new JButton("...");
        browseBtn.addActionListener(e -> choosePhoto());
        photoRow.add(browseBtn, BorderLayout.EAST);
        gbc.gridx = 1; fieldsPanel.add(photoRow, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        priorityBadge.setFont(UITheme.FONT_SMALL);
        priorityBadge.setForeground(UITheme.PRIMARY);
        fieldsPanel.add(priorityBadge, gbc);

        formCard.add(fieldsPanel, BorderLayout.CENTER);

        JButton submitBtn = UITheme.createPrimaryButton("Submit Issue");
        submitBtn.addActionListener(e -> submitIssue());
        formCard.add(submitBtn, BorderLayout.SOUTH);

        add(formCard, BorderLayout.WEST);

        // Right: Reported Issues Table & Tracking Card
        JPanel tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(8, 8));

        JPanel tableHeaderPanel = new JPanel(new BorderLayout());
        tableHeaderPanel.setBackground(Color.WHITE);
        JLabel tableHeader = new JLabel("My Submitted Issues & Progress Tracker");
        tableHeader.setFont(UITheme.FONT_TITLE);
        tableHeader.setForeground(UITheme.PRIMARY_DARK);
        tableHeaderPanel.add(tableHeader, BorderLayout.WEST);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionBtns.setBackground(Color.WHITE);
        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        JButton viewTimelineBtn = UITheme.createPrimaryButton("View Timeline / Status");

        refreshBtn.addActionListener(e -> loadIssues());
        viewTimelineBtn.addActionListener(e -> showSelectedTimeline());

        actionBtns.add(refreshBtn);
        actionBtns.add(viewTimelineBtn);
        tableHeaderPanel.add(actionBtns, BorderLayout.EAST);

        tableCard.add(tableHeaderPanel, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(issuesTable), BorderLayout.CENTER);

        add(tableCard, BorderLayout.CENTER);
    }

    private void choosePhoto() {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            photoField.setText(f.getAbsolutePath());
        }
    }

    private void submitIssue() {
        String title = titleField.getText().trim();
        Category cat = (Category) categoryCombo.getSelectedItem();
        String village = villageField.getText().trim();
        Urgency urg = (Urgency) urgencyCombo.getSelectedItem();
        String desc = descArea.getText().trim();
        String photo = photoField.getText().trim();

        if (title.isEmpty() || village.isEmpty() || desc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter title, village, and description.", 
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Issue issue = new Issue();
            issue.setReporterId(currentUser.getId());
            issue.setTitle(title);
            issue.setCategory(cat);
            issue.setVillageArea(village);
            issue.setUrgency(urg);
            issue.setDescription(desc);
            issue.setPhotoPath(photo);

            boolean ok = issueService.reportIssue(issue);
            if (ok) {
                JOptionPane.showMessageDialog(this, 
                        "Issue successfully registered! ID: #" + issue.getId() + "\nStatus: PENDING VERIFICATION", 
                        "Complaint Submitted", JOptionPane.INFORMATION_MESSAGE);
                titleField.setText("");
                descArea.setText("");
                photoField.setText("");
                loadIssues();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to submit: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadIssues() {
        tableModel.setRowCount(0);
        try {
            List<Issue> list = issueService.getIssuesByReporter(currentUser.getId());
            for (Issue i : list) {
                tableModel.addRow(new Object[]{
                        i.getId(),
                        i.getTitle(),
                        i.getCategory().getDisplayName(),
                        i.getVillageArea(),
                        i.getUrgency().getDisplayName(),
                        i.getStatus().getDisplayName(),
                        i.getPriorityScore(),
                        com.ruralhelp.util.DateTimeUtil.formatRelative(i.getCreatedAt())
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void showSelectedTimeline() {
        int row = issuesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an issue from the table first.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int issueId = (Integer) tableModel.getValueAt(row, 0);
        try {
            List<StatusHistory> history = statusHistoryDAO.findByIssueId(issueId);
            StringBuilder sb = new StringBuilder();
            sb.append("=== Timeline for Issue #").append(issueId).append(" ===\n\n");
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
            JOptionPane.showMessageDialog(this, "Error loading history: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

