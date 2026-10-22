package co.istad.visal.e_learning.features.content;

import co.istad.visal.e_learning.features.content.dto.*;

import java.util.List;
import java.util.UUID;

public interface ContentService {

    // ----- read -----

    /** Public curriculum outline. Titles and durations only. */
    List<SectionResponse> getCurriculum(UUID courseId);

    /** Gated. Runs CourseAccessService.requireCanViewContent first. */
    LessonContentResponse getLessonContent(UUID lessonId);

    // ----- sections -----

    SectionResponse createSection(CreateSectionRequest request);

    SectionResponse updateSection(UUID sectionId, UpdateSectionRequest request);

    void deleteSection(UUID sectionId);

    void reorderSections(UUID courseId, ReorderRequest request);

    // ----- lessons -----

    LessonContentResponse createLesson(CreateLessonRequest request);

    LessonContentResponse updateLesson(UUID lessonId, UpdateLessonRequest request);

    void deleteLesson(UUID lessonId);

    void reorderLessons(UUID sectionId, ReorderRequest request);

    // ----- files -----

    LessonFileResponse attachFile(UUID lessonId, String fileName, String storageKey, String mimeType, Long sizeBytes);

    void deleteFile(UUID fileId);
}
