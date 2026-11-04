package co.istad.visal.e_learning.features.report;

import co.istad.visal.e_learning.features.course.CourseRepository;
import co.istad.visal.e_learning.features.enrollment.EnrollmentRepository;
import co.istad.visal.e_learning.features.user.UserRepository;
import co.istad.visal.e_learning.security.CurrentUserService;
import co.istad.visal.e_learning.security.KeycloakRoleEnum;
import co.istad.visal.e_learning.security.KeycloakUserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformUserCountTest {

    private final UserRepository users = mock(UserRepository.class);
    private final ReportRepository reports = mock(ReportRepository.class);
    private final KeycloakUserService keycloakUsers = mock(KeycloakUserService.class);
    private final ReportServiceImpl service = new ReportServiceImpl(reports, mock(CourseRepository.class),
            mock(EnrollmentRepository.class), users, mock(CurrentUserService.class), keycloakUsers);

    @Test
    void countsKeycloakAccountsNotLocalRows() {
        when(users.count()).thenReturn(1L);
        when(keycloakUsers.countUsers(null)).thenReturn(5L);

        assertEquals(5, service.platformStats().totalUsers());
    }

    @Test
    void countsInstructorsByRoleNotCourseOwners() {
        when(reports.countInstructors()).thenReturn(0L);
        when(keycloakUsers.countUsersWithRole(KeycloakRoleEnum.INSTRUCTOR)).thenReturn(3L);

        assertEquals(3, service.platformStats().totalInstructors());
    }

    @Test
    void fallsBackToLocalInstructorsWhenKeycloakIsDown() {
        when(reports.countInstructors()).thenReturn(2L);
        when(keycloakUsers.countUsersWithRole(KeycloakRoleEnum.INSTRUCTOR)).thenThrow(new RuntimeException("down"));

        assertEquals(2, service.platformStats().totalInstructors());
    }

    @Test
    void fallsBackToLocalRowsWhenKeycloakIsDown() {
        when(users.count()).thenReturn(1L);
        when(keycloakUsers.countUsers(null)).thenThrow(new RuntimeException("down"));

        assertEquals(1, service.platformStats().totalUsers());
    }
}
