package com.abc.studentportal.common.persistence.dynamodb;

import com.abc.studentportal.common.application.DependencyChecker;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.application.DynamoCourseQueries;
import com.abc.studentportal.enrollment.application.EnrollmentRepository;
import com.abc.studentportal.instructor.application.DynamoInstructorQueries;
import com.abc.studentportal.student.application.DynamoStudentQueries;

import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoDependencyChecker implements DependencyChecker {

    private static final CursorRequest ONE = new CursorRequest(1, null);

    private final DynamoStudentQueries students;

    private final DynamoInstructorQueries instructors;

    private final DynamoCourseQueries courses;

    private final EnrollmentRepository enrollments;

    public DynamoDependencyChecker(DynamoStudentQueries students, DynamoInstructorQueries instructors,
                                   DynamoCourseQueries courses, EnrollmentRepository enrollments) {

        this.students = students;
        this.instructors = instructors;
        this.courses = courses;
        this.enrollments = enrollments;
    }

    @Override
    public boolean departmentHasDependents(UUID id) {

        return !students.findByDepartment(id, null, ONE).content().isEmpty()
                || !instructors.findByDepartment(id, ONE).content().isEmpty()
                || !courses.findByDepartment(id, ONE).content().isEmpty();
    }

    @Override
    public boolean studentHasEnrollmentHistory(UUID id) {

        return enrollments.existsByStudentId(id);
    }

    @Override
    public boolean instructorHasCourses(UUID id) {

        return !courses.findByInstructor(id, ONE).content().isEmpty();
    }

    @Override
    public boolean courseHasEnrollmentHistory(UUID id) {

        return enrollments.existsByCourseId(id);
    }

}
