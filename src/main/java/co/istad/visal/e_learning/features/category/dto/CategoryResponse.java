package co.istad.visal.e_learning.features.category.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * What leaves the API. Never return the entity itself — a JPA entity carries
 * lazy proxies and every column you later add, including ones you did not
 * mean to expose.
 */
public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        long courseCount,
        Instant createdAt
) {
}
