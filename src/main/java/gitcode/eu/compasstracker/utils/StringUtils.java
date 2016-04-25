package gitcode.eu.compasstracker.utils;

/**
 * Helper class for operations on strings
 */
public final class StringUtils {
    public static boolean isNullOrEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }
}
