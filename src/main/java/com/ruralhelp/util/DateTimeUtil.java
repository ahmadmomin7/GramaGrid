package com.ruralhelp.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Utility for date/time formatting.
 */
public class DateTimeUtil {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static String format(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        return DATE_FORMAT.format(timestamp);
    }

    public static String formatRelative(Timestamp timestamp) {
        if (timestamp == null) return "N/A";
        long diffMs = System.currentTimeMillis() - timestamp.getTime();
        long diffMinutes = diffMs / (60 * 1000);
        long diffHours = diffMinutes / 60;
        long diffDays = diffHours / 24;

        if (diffMinutes < 1) return "Just now";
        if (diffMinutes < 60) return diffMinutes + " min ago";
        if (diffHours < 24) return diffHours + " hr ago";
        if (diffDays == 1) return "1 day ago";
        return diffDays + " days ago";
    }
}
