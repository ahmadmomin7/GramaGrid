package com.ruralhelp.ui;

import com.ruralhelp.model.Category;
import com.ruralhelp.model.NGO;
import com.ruralhelp.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Registration dialog for new Community Members and NGOs.
 */
public class RegisterDialog extends JDialog {
    private final AuthService authService;
    private final JTabbedPane tabbedPane = new JTabbedPane();

    // Member fields
    private final JTextField memUser = new JTextField(15);
    private final JPasswordField memPass = new JPasswordField(15);
    private final JTextField memName = new JTextField(15);
    private final JTextField memPhone = new JTextField(15);
    private final JTextField memEmail = new JTextField(15);
    private final JTextField memVillage = new JTextField(15);

    // NGO fields
    private final JTextField ngoUser = new JTextField(15);
    private final JPasswordField ngoPass = new JPasswordField(15);
    private final JTextField ngoOrgName = new JTextField(15);
    private final JTextField ngoContact = new JTextField(15);
    private final JTextField ngoPhone = new JTextField(15);
    private final JTextField ngoEmail = new JTextField(15);
    private final JSpinner ngoCapacity = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
    private final JTextField ngoAreas = new JTextField("Rampur, Shivpur", 15);
    private final JTextArea ngoDesc = new JTextArea(3, 15);
    private final JCheckBox[] catBoxes;

    private boolean registered = false;

    public RegisterDialog(Frame owner, AuthService authService) {
        super(owner, "Register New Account", true);
        this.authService = authService;

        Category[] allCats = Category.values();
        catBoxes = new JCheckBox[allCats.length];
        for (int i = 0; i < allCats.length; i++) {
            catBoxes[i] = new JCheckBox(allCats[i].getDisplayName());
            if (i == 0) catBoxes[i].setSelected(true);
        }

        initUI();
    }

    private void initUI() {
        setSize(520, 520);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PRIMARY_DARK);
        header.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel title = new JLabel("Create Account - Rural Support Platform");
        title.setFont(UITheme.FONT_HEADER_MED);
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        tabbedPane.addTab("Community Member", createMemberTab());
        tabbedPane.addTab("NGO Organization", createNgoTab());
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createMemberTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel form = new JPanel(new GridLayout(6, 2, 8, 10));
        form.add(new JLabel("Full Name:")); form.add(memName);
        form.add(new JLabel("Username:")); form.add(memUser);
        form.add(new JLabel("Password:")); form.add(memPass);
        form.add(new JLabel("Phone:")); form.add(memPhone);
        form.add(new JLabel("Email:")); form.add(memEmail);
        form.add(new JLabel("Village / Area:")); form.add(memVillage);
        panel.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton regBtn = UITheme.createPrimaryButton("Register Member");
        regBtn.addActionListener(e -> registerMember());
        btnPanel.add(regBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createNgoTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Organization:"), gbc);
        gbc.gridx = 1; form.add(ngoOrgName, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Contact Person:"), gbc);
        gbc.gridx = 1; form.add(ngoContact, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; form.add(ngoUser, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; form.add(ngoPass, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1; form.add(ngoPhone, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; form.add(ngoEmail, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Max Capacity:"), gbc);
        gbc.gridx = 1; form.add(ngoCapacity, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Service Villages:"), gbc);
        gbc.gridx = 1; form.add(ngoAreas, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Specializations:"), gbc);
        JPanel specPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        for (JCheckBox cb : catBoxes) specPanel.add(cb);
        gbc.gridx = 1; form.add(specPanel, gbc);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton regBtn = UITheme.createPrimaryButton("Register NGO");
        regBtn.addActionListener(e -> registerNgo());
        btnPanel.add(regBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void registerMember() {
        try {
            String u = memUser.getText().trim();
            String p = new String(memPass.getPassword());
            String n = memName.getText().trim();
            if (u.isEmpty() || p.isEmpty() || n.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter name, username, and password.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            authService.registerCommunityMember(u, p, n, memPhone.getText().trim(), memEmail.getText().trim(), memVillage.getText().trim());
            registered = true;
            JOptionPane.showMessageDialog(this, "Registration successful! You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registerNgo() {
        try {
            String u = ngoUser.getText().trim();
            String p = new String(ngoPass.getPassword());
            String org = ngoOrgName.getText().trim();
            if (u.isEmpty() || p.isEmpty() || org.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter organization name, username, and password.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            NGO ngo = new NGO();
            ngo.setUsername(u);
            ngo.setOrganizationName(org);
            ngo.setContactPerson(ngoContact.getText().trim());
            ngo.setPhone(ngoPhone.getText().trim());
            ngo.setEmail(ngoEmail.getText().trim());
            ngo.setMaxCapacity((Integer) ngoCapacity.getValue());
            ngo.setDescription(ngoDesc.getText().trim());
            ngo.setVillageArea("Regional HQ");

            List<Category> selectedCats = new ArrayList<>();
            Category[] allCats = Category.values();
            for (int i = 0; i < allCats.length; i++) {
                if (catBoxes[i].isSelected()) {
                    selectedCats.add(allCats[i]);
                }
            }
            ngo.setSpecializations(selectedCats);

            List<String> areas = new ArrayList<>();
            String[] areaTokens = ngoAreas.getText().split("[,;]");
            for (String at : areaTokens) {
                if (!at.trim().isEmpty()) areas.add(at.trim());
            }
            ngo.setServiceAreas(areas);

            authService.registerNgo(ngo, p);
            registered = true;
            JOptionPane.showMessageDialog(this, "NGO registration successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isRegistered() {
        return registered;
    }
}

