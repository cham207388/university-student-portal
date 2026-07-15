package com.abc.studentportal.common.persistence.postgres;

import com.abc.studentportal.common.application.DependencyChecker;
import com.abc.studentportal.course.persistence.postgres.CourseJpaRepository;
import com.abc.studentportal.enrollment.persistence.postgres.EnrollmentJpaRepository;
import com.abc.studentportal.instructor.persistence.postgres.InstructorJpaRepository;
import com.abc.studentportal.student.persistence.postgres.StudentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Profile({"local-postgres", "test-postgres"})
public class PostgresDependencyChecker implements DependencyChecker {

    private final StudentJpaRepository students;

    private final InstructorJpaRepository instructors;

    private final CourseJpaRepository courses;

    private final EnrollmentJpaRepository enrollments;

    public boolean departmentHasDependents(UUID departmentId) {

        return students.existsByDepartment_Id(departmentId)
                || instructors.existsByDepartment_Id(departmentId)
                || courses.existsByDepartment_Id(departmentId);
    }

    public boolean studentHasEnrollmentHistory(UUID studentId) {

        return enrollments.existsByStudent_Id(studentId);
    }

    public boolean instructorHasCourses(UUID instructorId) {

        return courses.existsByInstructor_Id(instructorId);
    }

    public boolean courseHasEnrollmentHistory(UUID courseId) {

        return enrollments.existsByCourse_Id(courseId);
    }

}
