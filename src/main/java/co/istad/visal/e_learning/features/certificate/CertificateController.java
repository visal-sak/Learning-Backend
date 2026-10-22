package co.istad.visal.e_learning.features.certificate;

import co.istad.visal.e_learning.features.certificate.dto.CertificateResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CertificateController {

    private final CertificateService service;
    private final CertificateTrackingService tracking;

    public CertificateController(CertificateService service, CertificateTrackingService tracking) {
        this.tracking = tracking;
        this.service = service;
    }

    /** Public — anyone with the serial (an employer, say) can verify it without an account. */
    @GetMapping("/certificates/{serial}")
    public CertificateResponse verify(@PathVariable String serial) {
        return service.verify(serial);
    }

    @GetMapping("/my/certificates")
    public List<CertificateResponse> mine() {
        return service.findMine();
    }

    /** Issue (or return) the certificate for a finished course. Idempotent. */
    @PostMapping("/my/certificates/{courseId}")
    public CertificateResponse claim(@PathVariable UUID courseId) {
        return service.claim(courseId);
    }

    /** Staff: issued certificates, students who finished but aren't certified yet, per-course totals. */
    @GetMapping("/manage/certificates")
    public co.istad.visal.e_learning.features.certificate.dto.CertificateTrackingResponse tracking() {
        return tracking.overview();
    }
}
