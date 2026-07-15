package com.abc.studentportal.common.persistence.postgres;

import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.persistence.postgres.CourseEntity;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.department.persistence.postgres.DepartmentEntity;
import com.abc.studentportal.enrollment.domain.Enrollment;
import com.abc.studentportal.enrollment.persistence.postgres.EnrollmentEntity;
import com.abc.studentportal.instructor.domain.Instructor;
import com.abc.studentportal.instructor.persistence.postgres.InstructorEntity;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.persistence.postgres.StudentEntity;

public final class PostgresDomainMapper {

    private PostgresDomainMapper() {

    }

    public static Department department(DepartmentEntity e) {

        return new Department(e.getId(), e.getCode(), e.getName(), e.getDescription(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

    public static Student student(StudentEntity e) {

        return new Student(e.getId(), e.getStudentNumber(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getStatus(),
                e.getDepartment().getId(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

    public static Instructor instructor(InstructorEntity e) {

        return new Instructor(e.getId(), e.getEmployeeNumber(), e.getFirstName(), e.getLastName(), e.getEmail(),
                e.getDepartment().getId(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

    public static Course course(CourseEntity e) {

        return new Course(e.getId(), e.getCourseCode(), e.getTitle(), e.getDescription(), e.getCredits(), e.getCapacity(),
                e.getStatus(), e.getDepartment().getId(), e.getInstructor().getId(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

    public static Enrollment enrollment(EnrollmentEntity e) {

        return new Enrollment(e.getId(), e.getStudentId(), e.getCourseId(), e.getStatus(), e.getEnrolledAt(), e.getDroppedAt(),
                e.getFinalGrade(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

}
