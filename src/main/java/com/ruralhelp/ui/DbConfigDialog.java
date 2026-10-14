package com.ruralhelp.ui;

import com.ruralhelp.util.DatabaseConfig;
import com.ruralhelp.util.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Modal dialog to configure and test MySQL database credentials.
 */
public class DbConfigDialog extends JDialog {
    private final JTextField hostField = new JTextField("localhost", 15);
    private final JTextField portField = new JTextField("3306", 6);
    private final JTextField dbNameField = new JTextField("rural_support_db", 15);
    private final JTextField userField = new JTextField("root", 15);
    private final JPasswordField passField = new JPasswordField(15);
    private final JLabel statusLabel = new JLabel("Status: Ready to test");

    public DbConfigDialog(Frame owner) {
        super(owner, "Database Connection Settings", true);
        initUI();
    }

    private void initUI() {
        setSize(480, 360);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.PRIMARY_DARK);
        headerPanel.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel headerTitle = new JLabel("MySQL Database Configuration");
        headerTitle.setFont(UITheme.FONT_HEADER_MED);
        headerTitle.setForeground(Color.WHITE);
        headerPanel.add(headerTitle, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Populate from current config if available
        userField.setText(DatabaseConfig.getUser());
        passField.setText(DatabaseConfig.getPassword());

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1;
        formPanel.add(hostField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        formPanel.add(portField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Database Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(dbNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("MySQL User:"), gbc);
        gbc.gridx = 1;
        formPanel.add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("MySQL Password:"), gbc);
        gbc.gridx = 1;
        formPanel.add(passField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        statusLabel.setFont(UITheme.FONT_SMALL);
        formPanel.add(statusLabel, gbc);

        add(formPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));

        JButton testBtn = UITheme.createSecondaryButton("Test Connection");
        JButton saveBtn = UITheme.createPrimaryButton("Save & Connect");
        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");

        testBtn.addActionListener(e -> testSettings());
        saveBtn.addActionListener(e -> saveSettings());
        cancelBtn.addActionListener(e -> dispose());

        btnPanel.add(testBtn);
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private String buildUrl() {
        return "jdbc:mysql://" + hostField.getText().trim() + ":" + portField.getText().trim() 
                + "/" + dbNameField.getText().trim() 
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private void testSettings() {
        String url = buildUrl();
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        try {
            DatabaseConfig.saveConfig(url, user, pass);
            if (DatabaseConnection.testConnection()) {
                statusLabel.setText("Success! Successfully connected to MySQL.");
                statusLabel.setForeground(UITheme.COLOR_LOW);
            } else {
                statusLabel.setText("Failed: Could not establish connection.");
                statusLabel.setForeground(UITheme.COLOR_CRITICAL);
            }
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
            statusLabel.setForeground(UITheme.COLOR_CRITICAL);
        }
    }

    private void saveSettings() {
        testSettings();
        if (DatabaseConnection.testConnection()) {
            JOptionPane.showMessageDialog(this, "Database connection verified and saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            int choice = JOptionPane.showConfirmDialog(this, 
                    "Connection test failed. Save anyway?", "Warning", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                dispose();
            }
        }
    }
}

