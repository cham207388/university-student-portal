package com.abc.studentportal.course.persistence.postgres;

import com.abc.studentportal.common.persistence.postgres.PostgresVersions;
import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.department.persistence.postgres.DepartmentJpaRepository;
import com.abc.studentportal.instructor.persistence.postgres.InstructorJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
@RequiredArgsConstructor
@Profile({"local-postgres", "test-postgres", "migration"})
public class CoursePostgresRepository implements CourseRepository {

    private final CourseJpaRepository courseJpaRepository;

    private final DepartmentJpaRepository departmentJpaRepository;

    private final InstructorJpaRepository instructorJpaRepository;

    public Course create(Course course) {

        return toDomain(courseJpaRepository.save(toEntity(course)));
    }

    public Course update(Course course) {

        CourseEntity existing = courseJpaRepository.findById(course.id()).orElseThrow();
        PostgresVersions.require(CourseEntity.class, course.id(), course.version(), existing.getVersion());
        existing.updateDetails(course.courseCode(), course.title(), course.description(), course.credits(), course.capacity(), course.status(),
                departmentJpaRepository.getReferenceById(course.departmentId()),
                instructorJpaRepository.getReferenceById(course.instructorId()));
        existing.touch(course.updatedAt());
        return toDomain(courseJpaRepository.save(existing));
    }

    public Optional<Course> findById(UUID id) {

        return courseJpaRepository.findById(id).map(this::toDomain);
    }

    public Optional<Course> findByCourseCode(String courseCode) {

        return courseJpaRepository.findByCode(courseCode).map(this::toDomain);
    }

    public boolean existsByCourseCode(String courseCode) {

        return courseJpaRepository.findByCode(courseCode).isPresent();
    }

    public void delete(Course course) {

        CourseEntity existing = courseJpaRepository.findById(course.id()).orElseThrow();
        PostgresVersions.require(CourseEntity.class, course.id(), course.version(), existing.getVersion());
        courseJpaRepository.delete(existing);
        courseJpaRepository.flush();
    }

    private CourseEntity toEntity(Course course) {

        return new CourseEntity(course.id(), course.courseCode(), course.title(), course.description(), course.credits(),
                course.capacity(), course.status(), course.departmentId(), course.instructorId(), course.createdAt(),
                course.updatedAt(), course.version());
    }

    private Course toDomain(CourseEntity entity) {

        return new Course(entity.getId(), entity.getCourseCode(), entity.getTitle(), entity.getDescription(), entity.getCredits(), entity.getCapacity(), entity.getStatus(), entity.getDepartment().getId(), entity.getInstructor().getId(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
    }

}
