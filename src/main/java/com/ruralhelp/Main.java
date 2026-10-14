package com.ruralhelp;

import com.ruralhelp.cli.ConsoleApp;
import com.ruralhelp.ui.MainFrame;
import com.ruralhelp.util.DatabaseConnection;

import javax.swing.*;
import java.awt.GraphicsEnvironment;

/**
 * Main application launcher for the Rural Community Support Platform.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Starting Rural Community Support Platform...");

        // Diagnostic connection check
        System.out.println("Database check: " + DatabaseConnection.getDiagnostics());

        boolean cliMode = false;
        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg)) {
                cliMode = true;
                break;
            }
        }

        if (cliMode || GraphicsEnvironment.isHeadless()) {
            System.out.println("Launching Command Line Interface...");
            new ConsoleApp().run();
        } else {
            System.out.println("Launching Graphical User Interface (Swing)...");
            SwingUtilities.invokeLater(() -> {
                try {
                    MainFrame frame = new MainFrame();
                    frame.start();
                } catch (Exception e) {
                    System.err.println("GUI initialization failed: " + e.getMessage() + ". Falling back to CLI...");
                    new ConsoleApp().run();
                }
            });
        }
    }
}

