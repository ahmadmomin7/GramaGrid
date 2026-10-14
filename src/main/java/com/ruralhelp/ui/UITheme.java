package com.ruralhelp.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Modern UI styling, color palette, and component decorators
 * designed for the Rural Community Support Platform.
 */
public class UITheme {

    // Palette
    public static final Color PRIMARY_DARK    = new Color(20, 54, 41);      // Deep Forest Green
    public static final Color PRIMARY         = new Color(45, 106, 79);     // Green
    public static final Color PRIMARY_LIGHT   = new Color(82, 183, 136);    // Sage Accent
    public static final Color ACCENT_TEAL     = new Color(27, 154, 170);    // Teal
    public static final Color BG_LIGHT        = new Color(248, 249, 250);   // Off-white canvas
    public static final Color CARD_BG         = Color.WHITE;
    public static final Color TEXT_MAIN       = new Color(33, 37, 41);      // Charcoal
    public static final Color TEXT_MUTED      = new Color(108, 117, 125);   // Gray
    public static final Color BORDER_COLOR    = new Color(222, 226, 230);   // Subtle Border
    
    public static final Color COLOR_CRITICAL  = new Color(217, 4, 41);      // Bright Red
    public static final Color COLOR_HIGH      = new Color(247, 127, 0);     // Orange
    public static final Color COLOR_MEDIUM    = new Color(252, 191, 73);    // Amber
    public static final Color COLOR_LOW       = new Color(43, 147, 72);     // Soft Green
    public static final Color COLOR_RESOLVED   = new Color(26, 147, 111);

    // Fonts
    public static final Font FONT_HEADER_LARGE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADER_MED   = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_TITLE        = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY         = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD    = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL        = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_STAT_NUM     = new Font("Segoe UI", Font.BOLD, 28);

    public static void applyGlobalTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("Button.font", FONT_BODY_BOLD);
        UIManager.put("TextField.font", FONT_BODY);
        UIManager.put("ComboBox.font", FONT_BODY);
        UIManager.put("Table.font", FONT_BODY);
        UIManager.put("TableHeader.font", FONT_BODY_BOLD);
    }

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(new LineBorder(PRIMARY.darker(), 1, true), new EmptyBorder(8, 16, 8, 16)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(Color.WHITE);
        btn.setForeground(TEXT_MAIN);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(8, 16, 8, 16)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(COLOR_CRITICAL);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(new LineBorder(COLOR_CRITICAL.darker(), 1, true), new EmptyBorder(8, 16, 8, 16)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JPanel createCardPanel() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(12, 14, 12, 14)));
        return card;
    }

    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(28);
        table.setShowGrid(true);
        table.setGridColor(new Color(235, 238, 240));
        table.setSelectionBackground(new Color(216, 239, 226));
        table.setSelectionForeground(TEXT_MAIN);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BODY_BOLD);
        header.setBackground(new Color(241, 244, 245));
        header.setForeground(PRIMARY_DARK);
        header.setPreferredSize(new Dimension(header.getWidth(), 32));
    }

    public static JPanel createMetricCard(String title, String value, Color accentColor) {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(5, 5));
        card.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel titleLbl = new JLabel(title.toUpperCase());
        titleLbl.setFont(FONT_SMALL);
        titleLbl.setForeground(TEXT_MUTED);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(FONT_STAT_NUM);
        valLbl.setForeground(TEXT_MAIN);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valLbl, BorderLayout.CENTER);
        return card;
    }
}

