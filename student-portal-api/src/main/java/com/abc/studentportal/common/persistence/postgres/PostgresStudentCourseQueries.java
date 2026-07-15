package com.abc.studentportal.common.persistence.postgres;

import com.abc.studentportal.common.application.StudentCourseQueries;
import com.abc.studentportal.common.pagination.*;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.persistence.postgres.CourseJpaRepository;
import com.abc.studentportal.enrollment.persistence.postgres.EnrollmentJpaRepository;
import com.abc.studentportal.student.domain.Student;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Component @Profile({"local-postgres", "test-postgres"}) @Transactional(readOnly = true)
public class PostgresStudentCourseQueries extends PostgresPageSupport implements StudentCourseQueries {
    private final CourseJpaRepository courses;
    private final EnrollmentJpaRepository enrollments;
    public PostgresStudentCourseQueries(CourseJpaRepository courses, EnrollmentJpaRepository enrollments, PostgresCursorCodec cursors) { super(cursors); this.courses = courses; this.enrollments = enrollments; }
    public CursorPage<Course> findCoursesByStudent(UUID id, CursorRequest request) { return page("courses:student:" + id, request, p -> courses.findDistinctByStudent(id, p), PostgresDomainMapper::course); }
    public CursorPage<Student> findStudentsByCourse(UUID id, CursorRequest request) { return page("students:course:" + id, request, p -> enrollments.findDistinctStudentsByCourse(id, p), PostgresDomainMapper::student); }
}
