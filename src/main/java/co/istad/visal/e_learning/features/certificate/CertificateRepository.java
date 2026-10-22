package co.istad.visal.e_learning.features.certificate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    Optional<Certificate> findBySerialNo(String serialNo);

    Optional<Certificate> findByUserIdAndCourseId(UUID userId, UUID courseId);

    boolean existsBySerialNo(String serialNo);

    List<Certificate> findByUserIdOrderByIssuedAtDesc(UUID userId);

    long countByCourseId(UUID courseId);

    List<Certificate> findAllByOrderByIssuedAtDesc();

    List<Certificate> findByCourseIdInOrderByIssuedAtDesc(Collection<UUID> courseIds);
}
