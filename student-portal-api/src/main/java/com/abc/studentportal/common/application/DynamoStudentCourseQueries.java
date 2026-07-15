package com.abc.studentportal.common.application;

import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.student.domain.Student;

import java.util.UUID;

public interface DynamoStudentCourseQueries {

    CursorPage<Course> findCoursesByStudent(UUID studentId, CursorRequest request);

    CursorPage<Student> findStudentsByCourse(UUID courseId, CursorRequest request);

}
