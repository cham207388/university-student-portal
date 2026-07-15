package com.abc.studentportal.common.migration;

import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.application.DynamoCourseQueries;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.department.application.DynamoDepartmentQueries;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.enrollment.application.DynamoEnrollmentQueries;
import com.abc.studentportal.enrollment.application.EnrollmentRepository;
import com.abc.studentportal.instructor.application.DynamoInstructorQueries;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.student.application.DynamoStudentProfileQueries;
import com.abc.studentportal.student.application.DynamoStudentQueries;
import com.abc.studentportal.student.application.StudentProfileRepository;
import com.abc.studentportal.student.application.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamoToPostgresMigrationRunnerTest {

    @Test
    void migratesEverySourcePage() {
        Fixture fixture = new Fixture(false);
        Department first = department();
        Department second = department();
        when(fixture.dynamoDepartments.findAll(new CursorRequest(1, null)))
                .thenReturn(new CursorPage<>(List.of(first), 1, "next", true));
        when(fixture.dynamoDepartments.findAll(new CursorRequest(1, "next")))
                .thenReturn(new CursorPage<>(List.of(second), 1, null, false));
        when(fixture.departments.findById(any())).thenReturn(Optional.empty());

        fixture.runner.migrate();

        verify(fixture.departments).create(first);
        verify(fixture.departments).create(second);
        verify(fixture.dynamoDepartments).findAll(new CursorRequest(1, "next"));
        verify(fixture.tracker).checkpoint(any(), org.mockito.ArgumentMatchers.eq("DEPARTMENT"),
                org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.eq(0L), org.mockito.ArgumentMatchers.isNull());
        verify(fixture.tracker).complete(any(), org.mockito.ArgumentMatchers.eq(false));
    }

    @Test
    void dryRunDoesNotWrite() {
        Fixture fixture = new Fixture(true);
        Department department = department();
        when(fixture.dynamoDepartments.findAll(new CursorRequest(1, null)))
                .thenReturn(new CursorPage<>(List.of(department), 1, null, false));
        when(fixture.departments.findById(department.id())).thenReturn(Optional.empty());

        fixture.runner.migrate();

        verify(fixture.departments, never()).create(any());
    }

    private static Department department() {
        Department department = mock(Department.class);
        when(department.id()).thenReturn(UUID.randomUUID());
        return department;
    }

    private static final class Fixture {
        private final DynamoDepartmentQueries dynamoDepartments = mock(DynamoDepartmentQueries.class);
        private final DepartmentRepository departments = mock(DepartmentRepository.class);
        private final DynamoToPostgresMigrationRunner runner;
        private final MigrationTracker tracker = mock(MigrationTracker.class);

        private Fixture(boolean dryRun) {
            DynamoInstructorQueries dynamoInstructors = mock(DynamoInstructorQueries.class);
            DynamoStudentQueries dynamoStudents = mock(DynamoStudentQueries.class);
            DynamoCourseQueries dynamoCourses = mock(DynamoCourseQueries.class);
            DynamoEnrollmentQueries dynamoEnrollments = mock(DynamoEnrollmentQueries.class);
            empty(dynamoInstructors, dynamoStudents, dynamoCourses, dynamoEnrollments);
            when(tracker.start(dryRun, 1)).thenReturn(UUID.randomUUID());
            runner = new DynamoToPostgresMigrationRunner(dynamoDepartments, dynamoInstructors, departments,
                    mock(InstructorRepository.class), mock(ConfigurableApplicationContext.class), dynamoStudents,
                    mock(StudentRepository.class), mock(StudentProfileRepository.class),
                    mock(DynamoStudentProfileQueries.class), dynamoCourses, mock(CourseRepository.class),
                    dynamoEnrollments, mock(EnrollmentRepository.class), 1, dryRun, tracker);
        }

        private void empty(DynamoInstructorQueries instructors, DynamoStudentQueries students,
                DynamoCourseQueries courses, DynamoEnrollmentQueries enrollments) {
            when(instructors.findAll(any())).thenReturn(new CursorPage<>(List.of(), 1, null, false));
            when(students.findAll(any())).thenReturn(new CursorPage<>(List.of(), 1, null, false));
            when(courses.findAll(any())).thenReturn(new CursorPage<>(List.of(), 1, null, false));
            when(enrollments.findAll(any())).thenReturn(new CursorPage<>(List.of(), 1, null, false));
        }
    }
}
