package co.istad.visal.e_learning.features.certificate;

import co.istad.visal.e_learning.features.certificate.dto.CertificateResponse;
import co.istad.visal.e_learning.features.enrollment.Enrollment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateService {

    /** Public: look a certificate up by the serial printed on it. */
    CertificateResponse verify(String serialNo);

    /** The signed-in student's certificates, newest first. */
    List<CertificateResponse> findMine();

    /**
     * Issue the certificate for a course the student has finished (or return
     * the existing one). Fails with a clear message when not yet eligible.
     */
    CertificateResponse claim(UUID courseId);

    /**
     * Called after progress changes (last lesson completed, quiz passed).
     * Issues silently when the enrollment is now eligible; never throws for
     * "not eligible yet".
     */
    Optional<CertificateResponse> issueIfEligible(Enrollment enrollment);
}
