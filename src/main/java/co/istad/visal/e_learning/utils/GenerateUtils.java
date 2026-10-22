package co.istad.visal.ecommerce.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

public class GenerateUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern DUPLICATE_HYPHENS = Pattern.compile("-+");

    private static final String PREFIX = "SB13-SKU-";
    private static final Random RANDOM = new Random();

    public static String getProductCode() {
        // Generates a random integer from 0 to 999999
        int randomNumber = RANDOM.nextInt(1000000);

        // Formats the integer to always be 6 digits, padded with leading zeros
        return PREFIX + String.format("%06d", randomNumber);
    }

    public static String toSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        // 1. Convert whitespace to single hyphens
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");

        // 2. Normalize accents/diacritics (e.g., "café" -> "cafe")
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);

        // 3. Remove non-ASCII/non-alphanumeric characters except hyphens and underscores
        String slug = NONLATIN.matcher(normalized).replaceAll("");

        // 4. Remove duplicate hyphens and convert to lowercase
        slug = DUPLICATE_HYPHENS.matcher(slug).replaceAll("-");

        return slug.toLowerCase(Locale.ENGLISH);
    }
}
