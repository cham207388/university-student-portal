package com.abc.studentportal.instructor.application;

import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.instructor.domain.Instructor;

import java.util.UUID;

public interface InstructorQueries {

    CursorPage<Instructor> findAll(CursorRequest request);

    CursorPage<Instructor> findByDepartment(UUID departmentId, CursorRequest request);

}
