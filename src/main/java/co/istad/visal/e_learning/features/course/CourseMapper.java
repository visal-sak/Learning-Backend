package co.istad.visal.e_learning.features.course;

import co.istad.visal.e_learning.features.content.SectionMapper;
import co.istad.visal.e_learning.features.course.dto.CourseSummaryResponse;
import co.istad.visal.e_learning.features.user.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, SectionMapper.class})
public interface CourseMapper {

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "instructorName", source = "instructor.displayName")
    @Mapping(target = "lessonCount", expression = "java(course.totalLessons())")
    @Mapping(target = "totalMinutes", expression = "java(course.totalMinutes())")
    CourseSummaryResponse toSummary(Course course);

    // CourseDetailResponse is assembled in the service rather than here: it
    // needs the caller's enrollment and edit rights, which a mapper has no
    // business knowing about.
}
