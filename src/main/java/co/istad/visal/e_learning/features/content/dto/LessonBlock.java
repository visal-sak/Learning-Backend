package co.istad.visal.e_learning.features.content.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * One piece of a lesson page, written like a blog post:
 *
 *   heading   — text, level (2 or 3)
 *   paragraph — text (supports **bold**, `code` and [links](https://…) in the UI)
 *   callout   — text, variant: tip | note | warning
 *   code      — language, code, runnable, and optional html / css / js parts for the web playground
 *   image     — url (from the media library), caption
 *   list      — items, ordered (numbered when true, bullets otherwise)
 *   quote     — text, caption (who said it)
 *   table     — rows (first row is the header)
 *   divider   — nothing else
 *
 * Unused fields are left null. Stored as JSON on the lesson.
 */
public record LessonBlock(

        @NotBlank
        @Pattern(regexp = "heading|paragraph|callout|code|image|divider|list|quote|table", message = "Unknown block type")
        String type,

        @Size(max = 20_000)
        String text,

        @Min(2) @Max(3)
        Integer level,

        @Pattern(regexp = "tip|note|warning", message = "Callout must be tip, note or warning")
        String variant,

        @Size(max = 30)
        @Pattern(regexp = "[a-z0-9+#-]*", message = "Invalid code language")
        String language,

        @Size(max = 50_000)
        String code,

        Boolean runnable,

        /* Web playground companions: a code block in one of html / css / javascript can carry the
           other two parts, so "Try it Yourself" opens with HTML, CSS and JS tabs like a real editor. */
        @Size(max = 50_000)
        String html,

        @Size(max = 50_000)
        String css,

        @Size(max = 50_000)
        String js,

        @Size(max = 2_000)
        @Pattern(regexp = "^(https?://|/).*", message = "Image URL must start with http(s):// or /")
        String url,

        @Size(max = 300)
        String caption,

        @Size(max = 100, message = "A list can have at most 100 items")
        List<@Size(max = 2_000) String> items,

        Boolean ordered,

        @Size(max = 50, message = "A table can have at most 50 rows")
        List<@Size(max = 12, message = "A table can have at most 12 columns") List<@Size(max = 500) String>> rows
) {
}
