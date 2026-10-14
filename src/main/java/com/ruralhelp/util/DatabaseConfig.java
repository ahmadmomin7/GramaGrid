package com.ruralhelp.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Handles database configuration loading and persistence.
 */
public class DatabaseConfig {
    private static final String CONFIG_FILE = "db.properties";
    private static String url = "jdbc:mysql://localhost:3306/rural_support_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String user = "root";
    private static String password = "root";

    static {
        loadConfig();
    }

    public static synchronized void loadConfig() {
        Properties props = new Properties();
        boolean loaded = false;

        // Try reading from current directory first
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
                loaded = true;
            } catch (IOException ignored) {}
        }

        // If not found, try classpath
        if (!loaded) {
            try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (in != null) {
                    props.load(in);
                    loaded = true;
                }
            } catch (IOException ignored) {}
        }

        if (loaded) {
            url = props.getProperty("db.url", url);
            user = props.getProperty("db.user", user);
            password = props.getProperty("db.password", password);
        }
    }

    public static synchronized void saveConfig(String newUrl, String newUser, String newPassword) throws IOException {
        url = newUrl;
        user = newUser;
        password = newPassword;

        Properties props = new Properties();
        props.setProperty("db.url", url);
        props.setProperty("db.user", user);
        props.setProperty("db.password", password);

        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Rural Community Support Platform - Database Configuration");
        }
    }

    public static String getUrl() { return url; }
    public static String getUser() { return user; }
    public static String getPassword() { return password; }
}
