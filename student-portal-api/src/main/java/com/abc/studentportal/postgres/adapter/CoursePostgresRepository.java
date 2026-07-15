package com.abc.studentportal.postgres.adapter;

import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.postgres.entity.CourseEntity;
import com.abc.studentportal.postgres.repository.CourseJpaRepository;
import com.abc.studentportal.postgres.repository.DepartmentJpaRepository;
import com.abc.studentportal.postgres.repository.InstructorJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.*;

@Repository
@Profile({"local-postgres", "test-postgres"})
public class CoursePostgresRepository implements CourseRepository {

    private final CourseJpaRepository courseJpaRepository;
    private final DepartmentJpaRepository departmentJpaRepository;
    private final InstructorJpaRepository instructorJpaRepository;

    public CoursePostgresRepository(CourseJpaRepository courseJpaRepository, DepartmentJpaRepository departmentJpaRepository,
            InstructorJpaRepository instructorJpaRepository) {
        this.courseJpaRepository = courseJpaRepository;
        this.departmentJpaRepository = departmentJpaRepository;
        this.instructorJpaRepository = instructorJpaRepository;
    }

    public Course create(Course course) {
        return toDomain(courseJpaRepository.save(toEntity(course)));
    }

    public Course update(Course course) {
        CourseEntity existing = courseJpaRepository.findById(course.id()).orElseThrow();
        if (existing.getVersion() != course.version())
            throw new ObjectOptimisticLockingFailureException(CourseEntity.class, course.id());
        existing.updateDetails(course.courseCode(), course.title(), course.description(), course.credits(), course.capacity(), course.status(),
                departmentJpaRepository.getReferenceById(course.departmentId()),
                instructorJpaRepository.getReferenceById(course.instructorId()));
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
        courseJpaRepository.deleteById(course.id());
    }

    private CourseEntity toEntity(Course course) {
        return new CourseEntity(course.id(), course.courseCode(), course.title(), course.description(), course.credits(), course.capacity(), course.status(), course.departmentId(), course.instructorId());
    }

    private Course toDomain(CourseEntity entity) {
        return new Course(entity.getId(), entity.getCourseCode(), entity.getTitle(), entity.getDescription(), entity.getCredits(), entity.getCapacity(), entity.getStatus(), entity.getDepartment().getId(), entity.getInstructor().getId(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
    }

}
