package co.istad.visal.e_learning.features.ai;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The server-side check that an AI reply does not give a quiz answer away. The
 * prompt asks the model not to; this is what makes it true, because a model will
 * sometimes do it anyway.
 *
 * Matching whole phrases is not enough. "HyperText" with "Markup Language" later
 * in the sentence gives away "HyperText Markup Language" without ever containing
 * it, so an answer of several words counts as leaked once most of them appear.
 */
public final class AnswerGuard {

    /** Share of an answer's words that gives the rest away. */
    private static final double LEAK_SHARE = 0.65;
    /** Share of a question's words a message must repeat to count as asking it. */
    private static final double ASKED_SHARE = 0.6;

    private static final Set<String> FILLER = Set.of(
            "a", "an", "the", "of", "to", "in", "on", "at", "by", "for", "with", "and", "or", "is", "are", "was", "be",
            "it", "its", "this", "that", "these", "those", "as", "not", "no", "yes", "do", "does", "did", "what", "which",
            "who", "how", "why", "when", "where", "can", "you", "your", "me", "my", "i", "we", "please", "tell", "about",
            "should", "would", "will", "from", "into", "than", "then", "there", "their", "they", "has", "have", "had");

    private AnswerGuard() {
    }

    /**
     * True when the text gives away one of the answers. Words the question itself
     * already uses do not count: repeating them tells the student nothing.
     */
    public static boolean leaks(String text, List<String> answers, String question) {
        if (text == null || text.isBlank() || answers == null) return false;
        List<String> written = words(text);
        String joined = String.join("", written);
        Set<String> inQuestion = Set.copyOf(meaningfulWords(question));

        return answers.stream().anyMatch(answer -> {
            List<String> parts = meaningfulWords(answer).stream().filter(w -> !inQuestion.contains(w)).toList();
            if (parts.isEmpty()) return false;
            if (parts.size() == 1) return written.contains(parts.getFirst());
            if (joined.contains(String.join("", parts))) return true;
            long found = parts.stream().filter(w -> written.contains(w) || joined.contains(w)).count();
            return (double) found / parts.size() >= LEAK_SHARE;
        });
    }

    /** True when a student's message is, near enough, this quiz question. */
    public static boolean asks(String message, String question) {
        List<String> wanted = meaningfulWords(question);
        if (wanted.size() < 2) return false;
        Set<String> said = Set.copyOf(words(message));
        long found = wanted.stream().filter(said::contains).count();
        return (double) found / wanted.size() >= ASKED_SHARE;
    }

    /** Answers of several words, specific enough to check against any reply about the lesson. */
    public static boolean isDistinctive(String answer) {
        return meaningfulWords(answer).size() >= 2;
    }

    /** The words a rewrite must avoid, to tell the model exactly what it gave away. */
    public static String forbiddenWords(List<String> answers, String question) {
        Set<String> inQuestion = Set.copyOf(meaningfulWords(question));
        return answers.stream()
                .flatMap(answer -> meaningfulWords(answer).stream())
                .filter(w -> !inQuestion.contains(w))
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private static List<String> meaningfulWords(String text) {
        return words(text).stream()
                .filter(w -> w.length() >= 2 && !FILLER.contains(w))
                .distinct()
                .collect(Collectors.toList());
    }

    private static List<String> words(String text) {
        if (text == null) return List.of();
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}+#]+"))
                .filter(w -> !w.isBlank())
                .toList();
    }
}
