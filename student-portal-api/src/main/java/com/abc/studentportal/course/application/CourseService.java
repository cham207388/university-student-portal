package com.abc.studentportal.course.application;

import com.abc.studentportal.common.application.DependencyChecker;
import com.abc.studentportal.common.application.StudentCourseQueries;
import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.exception.InvalidRequestException;
import com.abc.studentportal.common.exception.ResourceNotFoundException;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.enrollment.application.EnrollmentQueries;
import com.abc.studentportal.enrollment.domain.Enrollment;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.domain.Instructor;
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
public class CourseService {

    private final CourseRepository courses;

    private final DepartmentRepository departments;

    private final InstructorRepository instructors;

    private final Clock clock;

    private final DependencyChecker dependencies;

    private final CourseQueries queries;

    private final EnrollmentQueries enrollments;

    private final StudentCourseQueries relationships;

    public Course create(CreateCommand command) {

        validateRelationships(command.departmentId(), command.instructorId());
        Instant now = clock.instant();
        return courses.create(new Course(UUID.randomUUID(), command.courseCode(), command.title(),
                command.description(),
                command.credits(), command.capacity(), command.status(), command.departmentId(), command.instructorId(),
                now, now, 0));
    }

    public Course update(UUID id, UpdateCommand command) {

        Course current = get(id);
        validateRelationships(command.departmentId(), command.instructorId());
        return courses
                .update(new Course(id, command.courseCode(), command.title(), command.description(), command.credits(),
                        command.capacity(), current.status(), command.departmentId(), command.instructorId(),
                        current.createdAt(),
                        clock.instant(), command.version()));
    }

    public Course changeStatus(UUID id, CourseStatus status, long version) {

        Course changed = get(id).transitionTo(status, clock.instant());
        return courses.update(new Course(changed.id(), changed.courseCode(), changed.title(), changed.description(),
                changed.credits(), changed.capacity(), changed.status(), changed.departmentId(), changed.instructorId(),
                changed.createdAt(), changed.updatedAt(), version));
    }

    public Course get(UUID id) {

        return courses.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course", id));
    }

    public Optional<Course> findByCourseCode(String courseCode) {

        return courses.findByCourseCode(
                com.abc.studentportal.common.domain.DomainChecks.uppercaseCode(courseCode, "courseCode"));
    }

    public CursorPage<Course> list(CourseListQuery query) {
        int filters = (query.departmentId() == null ? 0 : 1) + (query.instructorId() == null ? 0 : 1)
                + (query.status() == null ? 0 : 1) + (query.courseCode() == null ? 0 : 1);
        if (filters > 1)
            throw new InvalidRequestException("Course lists support one filter at a time");
        if (query.courseCode() != null) {
            if (query.cursor() != null)
                throw new InvalidRequestException("cursor cannot be combined with an exact courseCode lookup");
            return CursorPage.exact(findByCourseCode(query.courseCode()));
        }
        var request = new CursorRequest(query.limit(), query.cursor());
        if (query.departmentId() != null)
            return queries.findByDepartment(query.departmentId(), request);
        if (query.instructorId() != null)
            return queries.findByInstructor(query.instructorId(), request);
        if (query.status() != null)
            return queries.findByStatus(query.status(), request);
        return queries.findAll(request);
    }

    public CursorPage<Enrollment> listEnrollments(UUID courseId, Instant from, Instant to, CursorRequest request) {
        get(courseId);
        return enrollments.findByCourse(courseId, from, to, request);
    }

    public CursorPage<Student> listStudents(UUID courseId, CursorRequest request) {
        get(courseId);
        return relationships.findStudentsByCourse(courseId, request);
    }

    public void delete(UUID id, long version) {

        Course current = get(id);
        if (dependencies.courseHasEnrollmentHistory(id))
            throw new ConflictException("Course has enrollment history");
        courses.delete(new Course(current.id(), current.courseCode(), current.title(), current.description(),
                current.credits(),
                current.capacity(), current.status(), current.departmentId(), current.instructorId(),
                current.createdAt(),
                current.updatedAt(), version));
    }

    private void validateRelationships(UUID departmentId, UUID instructorId) {

        if (departments.findById(departmentId).isEmpty())
            throw new ResourceNotFoundException("Department", departmentId);
        Instructor instructor = instructors.findById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor", instructorId));
        if (!instructor.departmentId().equals(departmentId)) {
            throw new InvalidRequestException("Instructor must belong to the course department");
        }
    }

    public record CourseListQuery(UUID departmentId, UUID instructorId, CourseStatus status, String courseCode,
                                  int limit, String cursor) {

    }

    public record CreateCommand(String courseCode, String title, String description, int credits, int capacity,
                                CourseStatus status, UUID departmentId, UUID instructorId) {

    }

    public record UpdateCommand(String courseCode, String title, String description, int credits, int capacity,
                                UUID departmentId, UUID instructorId, long version) {

    }

}
