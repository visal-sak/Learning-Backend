package co.istad.visal.e_learning.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Turns a title into a URL-safe slug.
 *
 * Lives here rather than in one feature because both categories and courses
 * need it, and a second copy is a second place for the two to drift apart.
 */
public final class SlugUtils {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASH = Pattern.compile("(^-|-$)");

    private SlugUtils() {
    }

    public static String slugify(String input, String fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        String dashed = NON_ALNUM.matcher(normalized).replaceAll("-");
        String trimmed = EDGE_DASH.matcher(dashed).replaceAll("");

        return trimmed.isBlank() ? fallback : trimmed;
    }

    /**
     * Appends -2, -3 and so on until the slug is free.
     *
     * @param taken returns true when a slug is already in use
     */
    public static String unique(String input, String fallback, Predicate<String> taken) {
        String base = slugify(input, fallback);
        String candidate = base;
        int suffix = 2;

        while (taken.test(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
