package co.istad.visal.e_learning.features.certificate.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe to show to anyone holding the serial: it is exactly what is printed on
 * the certificate. No email, no user id.
 */
public record CertificateResponse(
        String serialNo,
        String studentName,
        UUID courseId,
        String courseTitle,
        String instructorName,
        Short quizScore,
        Instant issuedAt
) {
}
