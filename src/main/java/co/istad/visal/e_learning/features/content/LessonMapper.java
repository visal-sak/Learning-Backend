package co.istad.visal.e_learning.features.content;

import co.istad.visal.e_learning.features.content.dto.LessonFileResponse;
import co.istad.visal.e_learning.features.content.dto.LessonSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * There is no mapper method producing LessonContentResponse. That DTO is
 * assembled in the service, after the access check, with a freshly signed
 * URL. Keeping it out of the mapper means no caller can accidentally build
 * one without passing the gate first.
 */
@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(target = "locked", constant = "false")
    LessonSummaryResponse toSummary(Lesson lesson);

    @Mapping(target = "downloadUrl", ignore = true)
    LessonFileResponse toFileResponse(LessonFile file);
}
