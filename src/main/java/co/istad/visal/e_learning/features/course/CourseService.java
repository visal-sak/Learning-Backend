package co.istad.visal.e_learning.features.course;

import co.istad.visal.e_learning.utils.PageResponse;
import co.istad.visal.e_learning.features.course.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CourseService {

    /** Public catalog. Published courses only, whoever is asking. */
    PageResponse<CourseSummaryResponse> search(String q, UUID categoryId, CourseLevel level, Pageable pageable);

    /** Drafts resolve only for their owner or an admin. */
    CourseDetailResponse getById(UUID id);

    CourseDetailResponse getBySlug(String slug);

    /** Courses the caller teaches. */
    List<CourseSummaryResponse> findMine();

    /** Admin review queue, oldest submission first. */
    List<CourseSummaryResponse> findPendingReview();

    CourseDetailResponse create(CreateCourseRequest request);

    CourseDetailResponse update(UUID id, UpdateCourseRequest request);

    CourseDetailResponse changeStatus(UUID id, ChangeStatusRequest request);

    void delete(UUID id);
}
