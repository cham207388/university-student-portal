package com.abc.studentportal.student.application;

import com.abc.studentportal.common.application.DependencyChecker;
import com.abc.studentportal.common.application.StudentCourseQueries;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.enrollment.application.EnrollmentQueries;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * PostgreSQL transaction boundary for student and profile workflows.
 */
@Service
@Profile({"local-postgres", "test-postgres"})
@Transactional
public class PostgresStudentService extends StudentService {

    public PostgresStudentService(StudentRepository students, StudentProfileRepository profiles,
                                  DepartmentRepository departments, Clock clock,
                                  DependencyChecker dependencies, StudentQueries queries,
                                  EnrollmentQueries enrollments, StudentCourseQueries relationships) {

        super(students, profiles, departments, clock, dependencies, queries, enrollments, relationships);
    }

}
