package com.abc.studentportal.course.application;

import com.abc.studentportal.course.domain.Course;

import java.util.Optional;
import java.util.UUID;

public interface CourseRepository {

	Course save(Course course);

	Optional<Course> findById(UUID id);

	boolean existsByCourseCode(String courseCode);

	void delete(Course course);
}
