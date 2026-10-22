package co.istad.visal.e_learning.features.content;

import co.istad.visal.e_learning.features.content.dto.SectionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = LessonMapper.class)
public interface SectionMapper {

    @Mapping(target = "totalMinutes", expression = "java(section.totalMinutes())")
    SectionResponse toResponse(Section section);

    // toSummary lives in LessonMapper. Declaring it here too gives MapStruct
    // two equally valid candidates for the lesson list and it refuses to pick.
}