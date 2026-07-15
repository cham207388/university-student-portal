package com.abc.studentportal.course.application;

import com.abc.studentportal.common.exception.InvalidRequestException;
import com.abc.studentportal.common.exception.ResourceNotFoundException;
import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.application.DependencyChecker;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.domain.Instructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

@Service
@Profile({"local-dynamodb", "test-dynamodb"})
public class CourseService {

    private final CourseRepository courses;

    private final DepartmentRepository departments;

    private final InstructorRepository instructors;

    private final Clock clock;

    private final DependencyChecker dependencies;

    public CourseService(CourseRepository courses, DepartmentRepository departments,
                         InstructorRepository instructors, Clock clock, DependencyChecker dependencies) {
        this.courses = courses;
        this.departments = departments;
        this.instructors = instructors;
        this.clock = clock;
        this.dependencies = dependencies;
    }

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

    public record CreateCommand(String courseCode, String title, String description, int credits, int capacity,
                                CourseStatus status, UUID departmentId, UUID instructorId) {

    }

    public record UpdateCommand(String courseCode, String title, String description, int credits, int capacity,
                                UUID departmentId, UUID instructorId, long version) {

    }

}
