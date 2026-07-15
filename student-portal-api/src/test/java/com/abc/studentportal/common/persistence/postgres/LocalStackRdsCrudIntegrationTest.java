package com.abc.studentportal.common.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.student.application.StudentRepository;
import com.abc.studentportal.student.application.StudentProfileRepository;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentProfile;
import com.abc.studentportal.student.domain.StudentStatus;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.domain.Instructor;
import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;
import com.abc.studentportal.enrollment.application.EnrollmentRepository;
import com.abc.studentportal.enrollment.domain.Enrollment;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Tag("localstack-rds")
@SpringBootTest
@ActiveProfiles("local-postgres")
class LocalStackRdsCrudIntegrationTest {
    @Autowired DepartmentRepository departments;
    @Autowired StudentRepository students;
    @Autowired StudentProfileRepository studentProfiles;
    @Autowired InstructorRepository instructors;
    @Autowired CourseRepository courses;
    @Autowired EnrollmentRepository enrollments;

    @Test
    void departmentCrudRoundTripUsesLocalStackRds() {
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        Department created = departments.create(new Department(id, "RDS-" + id.toString().substring(0, 8),
                "RDS Integration", "LocalStack RDS", now, now, 0));
        assertThat(departments.findById(id)).contains(created);
        assertThat(departments.findByCode(created.code())).contains(created);
        departments.delete(created);
        assertThat(departments.findById(id)).isEmpty();
    }

    @Test
    void studentProfileCrudUsesSharedStudentPrimaryKey() {
        Instant creationTime = Instant.now();
        UUID departmentId = UUID.randomUUID();
        Department department = departments.create(new Department(departmentId,
                "PROF-" + departmentId.toString().substring(0, 8), "Profiles", "RDS",
                creationTime, creationTime, 0));
        UUID studentId = UUID.randomUUID();
        Student student = students.create(new Student(studentId,
                "STU-" + studentId.toString().substring(0, 8), "Katherine", "Johnson",
                "katherine-" + studentId + "@example.com", StudentStatus.ACTIVE, departmentId,
                creationTime, creationTime, 0));

        StudentProfile profile = studentProfiles.create(new StudentProfile(studentId, studentId,
                LocalDate.of(1918, 8, 26), "555-0100", "Original address", null,
                "unknown", "unknown", "unknown", "unknown",
                creationTime, creationTime, 0));

        assertThat(profile.id()).isEqualTo(studentId);
        assertThat(profile.studentId()).isEqualTo(studentId);
        assertThat(studentProfiles.findByStudentId(studentId)).contains(profile);

        StudentProfile updatedProfile = new StudentProfile(profile.id(), profile.studentId(),
                profile.dateOfBirth(), "555-0199", "Updated address", profile.addressLine2(),
                profile.city(), profile.state(), profile.postalCode(), profile.country(), profile.createdAt(),
                profile.updatedAt(), profile.version());
        StudentProfile persistedProfile = studentProfiles.update(updatedProfile);
        assertThat(persistedProfile.phoneNumber()).isEqualTo("555-0199");
        assertThat(persistedProfile.addressLine1()).isEqualTo("Updated address");
        assertThat(persistedProfile.version()).isGreaterThan(profile.version());

        studentProfiles.delete(persistedProfile);
        assertThat(studentProfiles.findByStudentId(studentId)).isEmpty();
        students.delete(student);
        departments.delete(department);
    }

    @Test
    void allRelationshipsAndUpdatesRoundTripThroughLocalStackRds() {
        Instant creationTime = Instant.now();
        UUID departmentId = UUID.randomUUID();
        Department department = departments.create(new Department(departmentId, "REL-" + departmentId.toString().substring(0, 8), "Relationships", "RDS", creationTime, creationTime, 0));
        UUID instructorId = UUID.randomUUID();
        Instructor instructor = instructors.create(new Instructor(instructorId, "EMP-" + instructorId.toString().substring(0, 8), "Ada", "Lovelace", "ada-" + instructorId + "@example.com", departmentId, creationTime, creationTime, 0));
        UUID courseId = UUID.randomUUID();
        Course course = courses.create(new Course(courseId, "CRS" + courseId.toString().substring(0, 5).toUpperCase(), "Integration", "RDS", 3, 20, CourseStatus.OPEN, departmentId, instructorId, creationTime, creationTime, 0));
        UUID studentId = UUID.randomUUID();
        Student student = students.create(new Student(studentId, "STU-" + studentId.toString().substring(0, 8), "Grace", "Hopper", "grace-" + studentId + "@example.com", StudentStatus.ACTIVE, departmentId, creationTime, creationTime, 0));
        Enrollment enrollment = enrollments.create(new Enrollment(UUID.randomUUID(), studentId, courseId, EnrollmentStatus.ENROLLED, creationTime, null, null, creationTime, creationTime, 0));

        assertThat(instructors.findByEmployeeNumber(instructor.employeeNumber())).contains(instructor);
        assertThat(courses.findByCourseCode(course.courseCode())).contains(course);
        assertThat(students.findByStudentNumber(student.studentNumber())).contains(student);
        assertThat(enrollments.existsActiveByStudentIdAndCourseId(studentId, courseId)).isTrue();

        Course updatedCourse = course.transitionTo(CourseStatus.CLOSED, Instant.now());
        Course persistedCourse = courses.update(updatedCourse);
        assertThat(persistedCourse.status()).isEqualTo(CourseStatus.CLOSED);
        assertThat(persistedCourse.createdAt()).isEqualTo(course.createdAt());
        assertThat(persistedCourse.version()).isGreaterThan(course.version());

    }
}
