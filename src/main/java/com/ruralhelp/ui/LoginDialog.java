package com.ruralhelp.ui;

import com.ruralhelp.model.User;
import com.ruralhelp.service.AuthService;
import com.ruralhelp.util.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Authentication dialog providing manual login and 1-click demo role switches.
 */
public class LoginDialog extends JDialog {
    private final AuthService authService;
    private final JTextField userField = new JTextField("admin", 15);
    private final JPasswordField passField = new JPasswordField("admin123", 15);
    private final JLabel errorLabel = new JLabel(" ");
    private User authenticatedUser;

    public LoginDialog(Frame owner, AuthService authService) {
        super(owner, "Sign In - Rural Community Support Platform", true);
        this.authService = authService;
        initUI();
    }

    private void initUI() {
        setSize(480, 420);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PRIMARY_DARK);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));
        JLabel title = new JLabel("Rural Community Support Platform");
        title.setFont(UITheme.FONT_HEADER_MED);
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Connecting rural communities with the right help");
        subtitle.setFont(UITheme.FONT_SMALL);
        subtitle.setForeground(UITheme.PRIMARY_LIGHT);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout());
        main.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 12));
        form.add(new JLabel("Username:"));
        form.add(userField);
        form.add(new JLabel("Password:"));
        form.add(passField);

        errorLabel.setForeground(UITheme.COLOR_CRITICAL);
        errorLabel.setFont(UITheme.FONT_SMALL);
        form.add(new JLabel(""));
        form.add(errorLabel);

        main.add(form, BorderLayout.NORTH);

        // Demo shortcuts panel
        JPanel demoPanel = UITheme.createCardPanel();
        demoPanel.setLayout(new BorderLayout(5, 5));
        JLabel demoTitle = new JLabel("Quick Demo Sign-In (1-Click):");
        demoTitle.setFont(UITheme.FONT_TITLE);
        demoTitle.setForeground(UITheme.PRIMARY_DARK);
        demoPanel.add(demoTitle, BorderLayout.NORTH);

        JPanel demoBtns = new JPanel(new GridLayout(1, 3, 6, 6));
        JButton btnAdmin = UITheme.createSecondaryButton("Admin");
        JButton btnNgo = UITheme.createSecondaryButton("NGO");
        JButton btnMember = UITheme.createSecondaryButton("Member");

        btnAdmin.setToolTipText("Login as District Admin (admin / admin123)");
        btnNgo.setToolTipText("Login as Jal Seva NGO (jalseva_ngo / ngo123)");
        btnMember.setToolTipText("Login as Ramesh Kumar (ramesh_k / user123)");

        btnAdmin.addActionListener(e -> { userField.setText("admin"); passField.setText("admin123"); doLogin(); });
        btnNgo.addActionListener(e -> { userField.setText("jalseva_ngo"); passField.setText("ngo123"); doLogin(); });
        btnMember.addActionListener(e -> { userField.setText("ramesh_k"); passField.setText("user123"); doLogin(); });

        demoBtns.add(btnAdmin);
        demoBtns.add(btnNgo);
        demoBtns.add(btnMember);
        demoPanel.add(demoBtns, BorderLayout.CENTER);

        main.add(demoPanel, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));

        JButton dbBtn = UITheme.createSecondaryButton("DB Settings");
        JButton regBtn = UITheme.createSecondaryButton("Register");
        JButton loginBtn = UITheme.createPrimaryButton("Sign In");

        dbBtn.addActionListener(e -> {
            DbConfigDialog dbDlg = new DbConfigDialog((Frame) getOwner());
            dbDlg.setVisible(true);
        });

        regBtn.addActionListener(e -> {
            RegisterDialog regDlg = new RegisterDialog((Frame) getOwner(), authService);
            regDlg.setVisible(true);
        });

        loginBtn.addActionListener(e -> doLogin());

        bottom.add(dbBtn);
        bottom.add(regBtn);
        bottom.add(loginBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private void doLogin() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter username and password.");
            return;
        }

        try {
            User user = authService.login(username, password);
            if (user != null) {
                this.authenticatedUser = user;
                dispose();
            } else {
                errorLabel.setText("Invalid credentials. Please try again.");
            }
        } catch (Exception ex) {
            errorLabel.setText("Database error: " + ex.getMessage());
            int choice = JOptionPane.showConfirmDialog(this, 
                    "Database connection failed:\n" + ex.getMessage() + "\n\nWould you like to configure MySQL settings now?",
                    "Connection Error", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                DbConfigDialog dbDlg = new DbConfigDialog((Frame) getOwner());
                dbDlg.setVisible(true);
            }
        }
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }
}

