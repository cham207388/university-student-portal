package com.abc.studentportal.course.application;

import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;
import java.util.UUID;

public interface CourseQueries {
    CursorPage<Course> findAll(CursorRequest request);
    CursorPage<Course> findByDepartment(UUID departmentId, CursorRequest request);
    CursorPage<Course> findByInstructor(UUID instructorId, CursorRequest request);
    CursorPage<Course> findByStatus(CourseStatus status, CursorRequest request);
}
