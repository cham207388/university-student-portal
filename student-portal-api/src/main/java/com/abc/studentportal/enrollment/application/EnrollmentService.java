package com.abc.studentportal.enrollment.application;

import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.exception.InvalidRequestException;
import com.abc.studentportal.common.exception.ResourceNotFoundException;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.enrollment.domain.Enrollment;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
import com.abc.studentportal.student.application.StudentRepository;
import com.abc.studentportal.student.domain.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Profile({"local-dynamodb", "test-dynamodb"})
public class EnrollmentService {

    private final EnrollmentRepository enrollments;

    private final StudentRepository students;

    private final CourseRepository courses;

    private final Clock clock;

    private final EnrollmentQueries queries;

    public Enrollment enroll(UUID studentId, UUID courseId) {

        Student student = students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        if (!student.mayEnroll())
            throw new ConflictException("Student is not active and cannot enroll");
        if (!course.acceptsEnrollment())
            throw new ConflictException("Course is not open for enrollment");
        Instant now = clock.instant();
        return enrollments.create(new Enrollment(UUID.randomUUID(), studentId, courseId, EnrollmentStatus.ENROLLED,
                now, null, null, now, now, 0));
    }

    public Enrollment changeStatus(UUID id, EnrollmentStatus target, String finalGrade, long version) {

        Enrollment current = get(id);
        Enrollment changed = current.transitionTo(target, finalGrade, clock.instant());
        return enrollments
                .update(new Enrollment(changed.id(), changed.studentId(), changed.courseId(), changed.status(),
                        changed.enrolledAt(), changed.droppedAt(), changed.finalGrade(), changed.createdAt(),
                        changed.updatedAt(), version));
    }

    public Enrollment drop(UUID id, long version) {

        return changeStatus(id, EnrollmentStatus.DROPPED, null, version);
    }

    public Enrollment get(UUID id) {

        return enrollments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
    }

    public CursorPage<Enrollment> list(EnrollmentListQuery query) {
        int filters = (query.studentId() == null ? 0 : 1) + (query.courseId() == null ? 0 : 1)
                + (query.status() == null ? 0 : 1);
        if (filters > 1)
            throw new InvalidRequestException(
                    "Enrollment lists support one relationship or status filter at a time");
        if ((query.enrolledFrom() != null || query.enrolledTo() != null) && query.studentId() == null
                && query.courseId() == null)
            throw new InvalidRequestException("Enrollment date ranges require studentId or courseId");
        var request = new CursorRequest(query.limit(), query.cursor());
        if (query.studentId() != null)
            return queries.findByStudent(query.studentId(), query.enrolledFrom(), query.enrolledTo(), request);
        if (query.courseId() != null)
            return queries.findByCourse(query.courseId(), query.enrolledFrom(), query.enrolledTo(), request);
        if (query.status() != null)
            return queries.findByStatus(query.status(), request);
        return queries.findAll(request);
    }

    public record EnrollmentListQuery(UUID studentId, UUID courseId, EnrollmentStatus status, Instant enrolledFrom,
                                      Instant enrolledTo, int limit, String cursor) {

    }

}
