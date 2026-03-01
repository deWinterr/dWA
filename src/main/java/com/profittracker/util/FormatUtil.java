package com.profittracker.util;

public class FormatUtil {

    public static String formatNumber(long number) {
        if (Math.abs(number) >= 1_000_000_000) {
            return String.format("%.2fB", number / 1_000_000_000.0);
        } else if (Math.abs(number) >= 1_000_000) {
            return String.format("%.2fM", number / 1_000_000.0);
        } else if (Math.abs(number) >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return formatWithCommas(number);
    }

    public static String formatWithCommas(long number) {
        return String.format("%,d", number);
    }

    public static String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }

    /**
     * Strips Minecraft formatting codes (section sign + character).
     */
    public static String stripFormatting(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
}
