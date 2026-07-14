package com.abc.studentportal.department.application;

import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.department.domain.Department;

public interface DynamoDepartmentQueries {
	CursorPage<Department> findAll(CursorRequest request);
}
