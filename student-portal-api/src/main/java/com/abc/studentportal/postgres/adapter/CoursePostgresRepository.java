package com.abc.studentportal.postgres.adapter;

import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.postgres.entity.CourseEntity;
import com.abc.studentportal.postgres.repository.CourseJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Profile({"local-postgres", "test-postgres"})
public class CoursePostgresRepository implements CourseRepository {

    private final CourseJpaRepository d;

    public CoursePostgresRepository(CourseJpaRepository d) {
        this.d = d;
    }

    public Course create(Course x) {
        return toDomain(d.save(toEntity(x)));
    }

    public Course update(Course x) {
        return toDomain(d.save(toEntity(x)));
    }

    public Optional<Course> findById(UUID id) {
        return d.findById(id).map(this::toDomain);
    }

    public Optional<Course> findByCourseCode(String c) {
        return d.findByCode(c).map(this::toDomain);
    }

    public boolean existsByCourseCode(String c) {
        return d.findByCode(c).isPresent();
    }

    public void delete(Course x) {
        d.deleteById(x.id());
    }

    private CourseEntity toEntity(Course x) {
        return new CourseEntity(x.id(), x.courseCode(), x.title(), x.description(), x.credits(), x.capacity(), x.status(), x.departmentId(), x.instructorId(), x.createdAt(), x.updatedAt());
    }

    private Course toDomain(CourseEntity e) {
        return new Course(e.getId(), e.getCourseCode(), e.getTitle(), e.getDescription(), e.getCredits(), e.getCapacity(), e.getStatus(), e.getDepartment().getId(), e.getInstructor().getId(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

}
