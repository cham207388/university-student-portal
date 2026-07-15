package com.abc.studentportal.course.application;

import com.abc.studentportal.common.application.DependencyChecker;
import com.abc.studentportal.common.application.StudentCourseQueries;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.enrollment.application.EnrollmentQueries;
import com.abc.studentportal.instructor.application.InstructorRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Profile({"local-postgres", "test-postgres"})
@Transactional
public class PostgresCourseService extends CourseService {

    public PostgresCourseService(CourseRepository courses, DepartmentRepository departments,
                                 InstructorRepository instructors, Clock clock, DependencyChecker dependencies,
                                 CourseQueries queries, EnrollmentQueries enrollments,
                                 StudentCourseQueries relationships) {

        super(courses, departments, instructors, clock, dependencies, queries, enrollments, relationships);
    }

}
