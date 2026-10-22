package co.istad.visal.e_learning.features.media.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record MediaResponse(
        UUID id,
        String name,
        String extension,
        String mediaType,
        Float size, // convert byte to mega byte
        String measurement, // MB
        String uri
) {
}
