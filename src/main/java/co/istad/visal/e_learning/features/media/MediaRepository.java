package co.istad.visal.e_learning.features.media;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    Page<Media> findByIsDraft(Boolean isDraft, Pageable pageable);

    Optional<Media> findByName(String name);
}