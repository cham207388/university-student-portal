package com.abc.studentportal.enrollment.application;

import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.student.application.StudentRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * PostgreSQL enrollment facade.  The complete enrollment operation runs in one
 * database transaction so relationship checks and the unique active-enrollment
 * constraint are evaluated atomically.
 */
@Service
@Profile({"local-postgres", "test-postgres"})
@Transactional
public class PostgresEnrollmentService extends EnrollmentService {

    public PostgresEnrollmentService(EnrollmentRepository enrollments,
                                     StudentRepository students,
                                     CourseRepository courses,
                                     Clock clock) {
        super(enrollments, students, courses, clock);
    }
}
