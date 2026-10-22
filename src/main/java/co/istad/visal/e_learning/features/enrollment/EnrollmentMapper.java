package co.istad.visal.e_learning.features.enrollment;

import org.mapstruct.Mapper;

/**
 * Deliberately thin. EnrollmentResponse needs a completed-lesson count and a
 * resume point, neither of which lives on the entity, so the service builds
 * it. A mapper that needs three extra arguments is not helping.
 */
@Mapper(componentModel = "spring")
public interface EnrollmentMapper {
}
