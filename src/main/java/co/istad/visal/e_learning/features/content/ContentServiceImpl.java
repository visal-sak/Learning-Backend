package co.istad.visal.e_learning.features.content;

import co.istad.visal.e_learning.expection.ConflictException;
import co.istad.visal.e_learning.expection.NotFoundException;
import co.istad.visal.e_learning.security.CourseAccessService;
import co.istad.visal.e_learning.features.content.dto.*;
import co.istad.visal.e_learning.features.course.Course;
import co.istad.visal.e_learning.features.course.CourseRepository;
import co.istad.visal.e_learning.features.enrollment.Enrollment;
import co.istad.visal.e_learning.features.enrollment.LessonProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ContentServiceImpl implements ContentService {

    /** Short enough that a shared link is useless by the time it is pasted. */
    private static final Duration SIGNED_URL_TTL = Duration.ofSeconds(60);

    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final LessonFileRepository fileRepository;
    private final CourseRepository courseRepository;
    private final LessonProgressRepository progressRepository;
    private final SectionMapper sectionMapper;
    private final LessonMapper lessonMapper;
    private final CourseAccessService access;
    private final StorageService storage;
    private final LessonBlocks lessonBlocks;

    public ContentServiceImpl(SectionRepository sectionRepository,
                              LessonRepository lessonRepository,
                              LessonFileRepository fileRepository,
                              CourseRepository courseRepository,
                              LessonProgressRepository progressRepository,
                              SectionMapper sectionMapper,
                              LessonMapper lessonMapper,
                              CourseAccessService access,
                              StorageService storage,
                              LessonBlocks lessonBlocks) {
        this.lessonBlocks = lessonBlocks;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.fileRepository = fileRepository;
        this.courseRepository = courseRepository;
        this.progressRepository = progressRepository;
        this.sectionMapper = sectionMapper;
        this.lessonMapper = lessonMapper;
        this.access = access;
        this.storage = storage;
    }

    // ---------------------------------------------------------------
    //  Reads
    // ---------------------------------------------------------------

    @Override
    public List<SectionResponse> getCurriculum(UUID courseId) {
        Course course = loadCourse(courseId);
        access.requireCanView(course);

        return sectionRepository.findByCourseIdOrderByPositionAsc(courseId).stream()
                .map(section -> new SectionResponse(
                        section.getId(),
                        section.getTitle(),
                        section.getPosition(),
                        section.totalMinutes(),
                        section.getLessons().stream()
                                .map(lesson -> new LessonSummaryResponse(
                                        lesson.getId(),
                                        lesson.getTitle(),
                                        lesson.getDurationMinutes(),
                                        lesson.isPreview(),
                                        lesson.getPosition(),
                                        !access.canViewContent(lesson),
                                        lesson.getKind(),
                                        lesson.getQuestionCount()))
                                .toList()))
                .toList();
    }

    /**
     * The most security-sensitive read in the system. Order: load, gate, then
     * build a response with a freshly signed URL.
     */
    @Override
    public LessonContentResponse getLessonContent(UUID lessonId) {
        Lesson lesson = lessonRepository.findWithCourse(lessonId)
                .orElseThrow(() -> new NotFoundException("No lesson with id " + lessonId));

        access.requireCanViewContent(lesson);

        Course course = lesson.getSection().getCourse();
        Optional<Enrollment> enrollment = access.activeEnrollment(course);

        boolean completed = enrollment
                .map(e -> progressRepository.existsByEnrollmentIdAndLessonId(e.getId(), lessonId))
                .orElse(false);

        List<LessonFileResponse> files = fileRepository.findByLessonId(lessonId).stream()
                .map(file -> new LessonFileResponse(
                        file.getId(),
                        file.getFileName(),
                        file.getMimeType(),
                        file.getSizeBytes(),
                        storage.signedUrl(file.getStorageKey(), SIGNED_URL_TTL)))
                .toList();

        return new LessonContentResponse(
                lesson.getId(),
                course.getId(),
                lesson.getTitle(),
                lesson.getContent(),
                lessonBlocks.read(lesson.getBlocks()),
                lesson.getVideoUrl() == null ? null : storage.signedUrl(lesson.getVideoUrl(), SIGNED_URL_TTL),
                lesson.getDurationMinutes(),
                lesson.isPreview(),
                completed,
                files,
                nextLessonId(course, lesson),
                lesson.getKind(),
                lesson.getQuestionCount());
    }

    // ---------------------------------------------------------------
    //  Sections
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public SectionResponse createSection(CreateSectionRequest request) {
        Course course = loadCourse(request.courseId());
        access.requireCanEdit(course);

        int position = (int) sectionRepository.countByCourseId(course.getId());
        Section section = new Section(course, request.title().trim(), position);

        return sectionMapper.toResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional
    public SectionResponse updateSection(UUID sectionId, UpdateSectionRequest request) {
        Section section = loadSection(sectionId);
        access.requireCanEdit(section.getCourse());

        section.setTitle(request.title().trim());
        return sectionMapper.toResponse(section);
    }

    @Override
    @Transactional
    public void deleteSection(UUID sectionId) {
        Section section = loadSection(sectionId);
        access.requireCanEdit(section.getCourse());

        if (!section.getLessons().isEmpty()) {
            throw new ConflictException("Remove the lessons in this section before deleting it");
        }

        sectionRepository.delete(section);
    }

    @Override
    @Transactional
    public void reorderSections(UUID courseId, ReorderRequest request) {
        Course course = loadCourse(courseId);
        access.requireCanEdit(course);

        List<Section> sections = sectionRepository.findByCourseIdOrderByPositionAsc(courseId);

        if (sections.size() != request.orderedIds().size()) {
            throw new ConflictException("Send every section id, in the order you want them saved");
        }

        for (int i = 0; i < request.orderedIds().size(); i++) {
            UUID id = request.orderedIds().get(i);
            Section section = sections.stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Section " + id + " is not in this course"));
            section.setPosition(i);
        }
    }

    // ---------------------------------------------------------------
    //  Lessons
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public LessonContentResponse createLesson(CreateLessonRequest request) {
        Section section = loadSection(request.sectionId());
        access.requireCanEdit(section.getCourse());

        int position = (int) lessonRepository.countBySectionId(section.getId());
        Lesson lesson = new Lesson(section, request.title().trim(), position);
        lesson.setContent(request.content());
        lesson.setBlocks(lessonBlocks.write(request.blocks()));
        lesson.setVideoUrl(request.videoUrl());
        lesson.setDurationMinutes(request.durationMinutes());
        lesson.setPreview(request.preview());
        lesson.setKind(request.kind());

        return getLessonContent(lessonRepository.save(lesson).getId());
    }

    @Override
    @Transactional
    public LessonContentResponse updateLesson(UUID lessonId, UpdateLessonRequest request) {
        Lesson lesson = loadLesson(lessonId);
        access.requireCanEdit(lesson.getSection().getCourse());

        lesson.setTitle(request.title().trim());
        lesson.setContent(request.content());
        lesson.setBlocks(lessonBlocks.write(request.blocks()));
        if (request.kind() != null) lesson.setKind(request.kind());
        lesson.setVideoUrl(request.videoUrl());
        lesson.setDurationMinutes(request.durationMinutes());
        lesson.setPreview(request.preview());

        return getLessonContent(lessonId);
    }

    @Override
    @Transactional
    public void deleteLesson(UUID lessonId) {
        Lesson lesson = loadLesson(lessonId);
        access.requireCanEdit(lesson.getSection().getCourse());
        lessonRepository.delete(lesson);
    }

    @Override
    @Transactional
    public void reorderLessons(UUID sectionId, ReorderRequest request) {
        Section section = loadSection(sectionId);
        access.requireCanEdit(section.getCourse());

        List<Lesson> lessons = lessonRepository.findBySectionIdOrderByPositionAsc(sectionId);

        if (lessons.size() != request.orderedIds().size()) {
            throw new ConflictException("Send every lesson id, in the order you want them saved");
        }

        for (int i = 0; i < request.orderedIds().size(); i++) {
            UUID id = request.orderedIds().get(i);
            Lesson lesson = lessons.stream()
                    .filter(l -> l.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Lesson " + id + " is not in this section"));
            lesson.setPosition(i);
        }
    }

    // ---------------------------------------------------------------
    //  Files
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public LessonFileResponse attachFile(UUID lessonId, String fileName, String storageKey,
                                         String mimeType, Long sizeBytes) {
        Lesson lesson = loadLesson(lessonId);
        access.requireCanEdit(lesson.getSection().getCourse());

        LessonFile file = new LessonFile(lesson, fileName, storageKey);
        file.setMimeType(mimeType);
        file.setSizeBytes(sizeBytes);
        fileRepository.save(file);

        return new LessonFileResponse(file.getId(), fileName, mimeType, sizeBytes,
                storage.signedUrl(storageKey, SIGNED_URL_TTL));
    }

    @Override
    @Transactional
    public void deleteFile(UUID fileId) {
        LessonFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("No file with id " + fileId));

        access.requireCanEdit(file.getLesson().getSection().getCourse());
        fileRepository.delete(file);
    }

    // ---------------------------------------------------------------

    private UUID nextLessonId(Course course, Lesson current) {
        List<Lesson> all = lessonRepository.findAllByCourse(course.getId());
        int index = all.indexOf(current);
        return (index >= 0 && index + 1 < all.size()) ? all.get(index + 1).getId() : null;
    }

    private Course loadCourse(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No course with id " + id));
    }

    private Section loadSection(UUID id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No section with id " + id));
    }

    private Lesson loadLesson(UUID id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No lesson with id " + id));
    }
}
