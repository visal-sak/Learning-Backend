package co.istad.visal.e_learning.features.certificate.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Staff view of certificates: who has one, who finished a course but doesn't yet,
 * and per-course totals. Unlike CertificateResponse this includes the student's email.
 */
public record CertificateTrackingResponse(
        Summary summary,
        List<CourseRow> courses,
        List<Issued> certificates,
        List<Awaiting> awaiting
) {
    public record Summary(long issued, long issuedThisMonth, long awaiting, long finishedStudents) {
    }

    /** enrolled → finished (all lessons) → certified; awaiting = finished but no certificate yet. */
    public record CourseRow(UUID courseId, String courseTitle, String instructorName, boolean hasQuiz,
                            long enrolled, long finished, long certified, long awaiting) {
    }

    public record Issued(String serialNo, UUID userId, String studentName, String email,
                         UUID courseId, String courseTitle, String instructorName,
                         Short quizScore, Instant issuedAt) {
    }

    /**
     * reason: QUIZ_NOT_TAKEN (has a quiz, no attempt yet), QUIZ_NOT_PASSED (attempted, below pass mark),
     * NOT_CLAIMED (requirements met, certificate just not generated yet).
     */
    public record Awaiting(UUID userId, String studentName, String email, UUID courseId, String courseTitle,
                           Instant finishedAt, Short bestQuizScore, Short passMark, int attempts, String reason) {
    }
}
