package com.ruralhelp.ui;

import com.ruralhelp.model.NGO;
import com.ruralhelp.model.User;
import com.ruralhelp.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Main application window for the Rural Community Support Platform.
 * Dynamically presents role-specific interfaces for Admin, NGO, and Community Members.
 */
public class MainFrame extends JFrame {
    private final AuthService authService = new AuthService();
    private User currentUser;

    private final JPanel headerPanel = new JPanel(new BorderLayout());
    private final JLabel userBadgeLabel = new JLabel();
    private final JPanel contentContainer = new JPanel(new BorderLayout());

    public MainFrame() {
        super("Rural Community Support Platform");
        UITheme.applyGlobalTheme();
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Header Banner
        headerPanel.setBackground(UITheme.PRIMARY_DARK);
        headerPanel.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setBackground(UITheme.PRIMARY_DARK);
        JLabel title = new JLabel("Rural Community Support Platform");
        title.setFont(UITheme.FONT_HEADER_LARGE);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Turning local problems into verified, trackable NGO action");
        subtitle.setFont(UITheme.FONT_SMALL);
        subtitle.setForeground(UITheme.PRIMARY_LIGHT);
        titleBlock.add(title);
        titleBlock.add(subtitle);
        headerPanel.add(titleBlock, BorderLayout.WEST);

        JPanel userControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        userControls.setBackground(UITheme.PRIMARY_DARK);

        userBadgeLabel.setFont(UITheme.FONT_BODY_BOLD);
        userBadgeLabel.setForeground(Color.WHITE);

        JButton switchUserBtn = UITheme.createSecondaryButton("Switch User / Logout");
        JButton dbSettingsBtn = UITheme.createSecondaryButton("DB Settings");

        switchUserBtn.addActionListener(e -> promptLogin());
        dbSettingsBtn.addActionListener(e -> {
            DbConfigDialog dlg = new DbConfigDialog(this);
            dlg.setVisible(true);
        });

        userControls.add(userBadgeLabel);
        userControls.add(dbSettingsBtn);
        userControls.add(switchUserBtn);
        headerPanel.add(userControls, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(contentContainer, BorderLayout.CENTER);
    }

    public void start() {
        setVisible(true);
        promptLogin();
    }

    public void promptLogin() {
        LoginDialog dlg = new LoginDialog(this, authService);
        dlg.setVisible(true);
        User user = dlg.getAuthenticatedUser();
        if (user != null) {
            setUserSession(user);
        } else if (currentUser == null) {
            // User cancelled without logging in and no session exists
            System.exit(0);
        }
    }

    public void setUserSession(User user) {
        this.currentUser = user;
        String badgeText = "Logged in: " + user.getFullName() + " [" + user.getRole().getDisplayName() + "]";
        if (user.getVillageArea() != null && !user.getVillageArea().isEmpty()) {
            badgeText += " (" + user.getVillageArea() + ")";
        }
        userBadgeLabel.setText(badgeText);

        contentContainer.removeAll();
        switch (user.getRole()) {
            case ADMIN:
                contentContainer.add(new AdminPanel(user), BorderLayout.CENTER);
                break;
            case NGO:
                if (user instanceof NGO) {
                    contentContainer.add(new NgoPanel((NGO) user), BorderLayout.CENTER);
                } else {
                    JOptionPane.showMessageDialog(this, "NGO profile details could not be loaded.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                break;
            case COMMUNITY_MEMBER:
                contentContainer.add(new CommunityMemberPanel(user), BorderLayout.CENTER);
                break;
        }

        contentContainer.revalidate();
        contentContainer.repaint();
    }
}

