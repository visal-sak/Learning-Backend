package co.istad.visal.e_learning.features.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    /** Used when renaming: the name may collide with any category other than this one. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    /**
     * How many courses point at this category. Deletion is blocked while this
     * is greater than zero — the FK is RESTRICT, but checking here lets us
     * return a clear 409 instead of a database error.
     */
    @Query("SELECT COUNT(c) FROM Course c WHERE c.category.id = :categoryId")
    long countCourses(@Param("categoryId") UUID categoryId);
}
