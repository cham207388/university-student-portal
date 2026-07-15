package com.abc.studentportal.instructor.application;

import com.abc.studentportal.common.application.DependencyChecker;
import com.abc.studentportal.course.application.CourseQueries;
import com.abc.studentportal.department.application.DepartmentRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Profile({"local-postgres", "test-postgres"})
@Transactional
public class PostgresInstructorService extends InstructorService {

    public PostgresInstructorService(InstructorRepository instructors, DepartmentRepository departments,
                                     Clock clock, DependencyChecker dependencies, InstructorQueries queries,
                                     CourseQueries courses) {

        super(instructors, departments, clock, dependencies, queries, courses);
    }

}
