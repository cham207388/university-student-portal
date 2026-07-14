package com.abc.studentportal.course.api;

import com.abc.studentportal.course.domain.Course;

public final class CourseMapper {

	private CourseMapper() {
	}

	public static CourseApi.Response toResponse(Course course) {
		return new CourseApi.Response(course.id(), course.courseCode(), course.title(), course.description(),
				course.credits(), course.capacity(), course.status(), course.departmentId(), course.instructorId(),
				course.createdAt(), course.updatedAt(), course.version());
	}
}
