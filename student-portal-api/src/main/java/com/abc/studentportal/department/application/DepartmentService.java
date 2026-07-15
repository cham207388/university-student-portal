package com.abc.studentportal.department.application;

import com.abc.studentportal.common.application.DependencyChecker;
import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.exception.InvalidRequestException;
import com.abc.studentportal.common.exception.ResourceNotFoundException;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.application.CourseQueries;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.instructor.application.InstructorQueries;
import com.abc.studentportal.instructor.domain.Instructor;
import com.abc.studentportal.student.application.StudentQueries;
import com.abc.studentportal.student.domain.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Profile({"local-dynamodb", "test-dynamodb"})
public class DepartmentService {

    private final DepartmentRepository repository;

    private final Clock clock;

    private final DependencyChecker dependencies;

    private final DepartmentQueries queries;

    private final StudentQueries students;

    private final InstructorQueries instructors;

    private final CourseQueries courses;

    public Department create(CreateCommand command) {

        Instant now = clock.instant();
        return repository
                .create(new Department(UUID.randomUUID(), command.code(), command.name(), command.description(),
                        now, now, 0));
    }

    public Department update(UUID id, UpdateCommand command) {

        Department current = get(id);
        return repository.update(new Department(id, command.code(), command.name(), command.description(),
                current.createdAt(), clock.instant(), command.version()));
    }

    public Department get(UUID id) {

        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    public Optional<Department> findByCode(String code) {

        return repository.findByCode(com.abc.studentportal.common.domain.DomainChecks.uppercaseCode(code, "code"));
    }

    public CursorPage<Department> list(DepartmentListQuery query) {
        if (query.code() != null) {
            if (query.cursor() != null)
                throw new InvalidRequestException("cursor cannot be combined with exact code lookup");
            return CursorPage.exact(findByCode(query.code()));
        }
        return queries.findAll(new CursorRequest(query.limit(), query.cursor()));
    }

    public CursorPage<Student> listStudents(UUID departmentId, String lastName, CursorRequest request) {
        get(departmentId);
        return students.findByDepartment(departmentId, lastName, request);
    }

    public CursorPage<Instructor> listInstructors(UUID departmentId, CursorRequest request) {
        get(departmentId);
        return instructors.findByDepartment(departmentId, request);
    }

    public CursorPage<Course> listCourses(UUID departmentId, CursorRequest request) {
        get(departmentId);
        return courses.findByDepartment(departmentId, request);
    }

    public void delete(UUID id, long version) {

        Department current = get(id);
        if (dependencies.departmentHasDependents(id))
            throw new ConflictException("Department still has dependent records");
        repository.delete(new Department(current.id(), current.code(), current.name(), current.description(),
                current.createdAt(), current.updatedAt(), version));
    }

    public record DepartmentListQuery(String code, int limit, String cursor) {

    }

    public record CreateCommand(String code, String name, String description) {

    }

    public record UpdateCommand(String code, String name, String description, long version) {

    }

}
