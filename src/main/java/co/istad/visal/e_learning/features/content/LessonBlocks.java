package co.istad.visal.e_learning.features.content;

import co.istad.visal.e_learning.features.content.dto.LessonBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.stream.Collectors;

/** Converts a lesson's blocks to and from the JSON stored in lessons.blocks. */
@Component
public class LessonBlocks {

    private static final Logger log = LoggerFactory.getLogger(LessonBlocks.class);
    private static final TypeReference<List<LessonBlock>> LIST = new TypeReference<>() {};

    private final JsonMapper json;

    public LessonBlocks(JsonMapper json) {
        this.json = json;
    }

    /** null / empty list → null column, so old lessons and "no blocks" look the same. */
    public String write(List<LessonBlock> blocks) {
        return blocks == null || blocks.isEmpty() ? null : json.writeValueAsString(blocks);
    }

    public List<LessonBlock> read(String stored) {
        if (stored == null || stored.isBlank()) return List.of();
        try {
            return json.readValue(stored, LIST);
        } catch (JacksonException e) {
            log.warn("Unreadable lesson blocks, showing none: {}", e.getMessage());
            return List.of();
        }
    }

    /** Plain-text version of the blocks, e.g. for the AI tutor's context. */
    public String toPlainText(String stored) {
        return read(stored).stream()
                .map(b -> switch (b.type()) {
                    case "heading" -> "## " + nz(b.text());
                    case "callout" -> nz(b.variant()).toUpperCase() + ": " + nz(b.text());
                    case "code" -> "```" + nz(b.language()) + "\n" + nz(b.code()) + "\n```"
                            + part("html", b.html()) + part("css", b.css()) + part("javascript", b.js());
                    case "image" -> b.caption() == null ? "" : "[Image: " + b.caption() + "]";
                    case "list" -> listText(b);
                    case "quote" -> "> " + nz(b.text()) + (b.caption() == null ? "" : " — " + b.caption());
                    case "table" -> b.rows() == null ? "" : b.rows().stream()
                            .map(r -> "| " + String.join(" | ", r) + " |")
                            .collect(Collectors.joining("\n"));
                    case "divider" -> "";
                    default -> nz(b.text());
                })
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    private static String listText(LessonBlock b) {
        if (b.items() == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.items().size(); i++) {
            sb.append(Boolean.TRUE.equals(b.ordered()) ? (i + 1) + ". " : "- ").append(b.items().get(i)).append("\n");
        }
        return sb.toString().trim();
    }

    private static String part(String language, String code) {
        return code == null || code.isBlank() ? "" : "\n```" + language + "\n" + code + "\n```";
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
