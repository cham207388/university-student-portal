package com.abc.studentportal.department.application;

import com.abc.studentportal.common.application.DependencyChecker;
import com.abc.studentportal.course.application.CourseQueries;
import com.abc.studentportal.instructor.application.InstructorQueries;
import com.abc.studentportal.student.application.StudentQueries;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Profile({"local-postgres", "test-postgres"})
@Transactional
public class PostgresDepartmentService extends DepartmentService {

    public PostgresDepartmentService(DepartmentRepository repository, Clock clock, DependencyChecker dependencies,
                                     DepartmentQueries queries, StudentQueries students,
                                     InstructorQueries instructors, CourseQueries courses) {

        super(repository, clock, dependencies, queries, students, instructors, courses);
    }

}
