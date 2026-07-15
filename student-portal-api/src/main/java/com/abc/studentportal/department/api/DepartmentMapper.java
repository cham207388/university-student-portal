package com.abc.studentportal.department.api;

import com.abc.studentportal.department.domain.Department;

public final class DepartmentMapper {

    private DepartmentMapper() {

    }

    public static DepartmentApi.Response toResponse(Department department) {

        return new DepartmentApi.Response(department.id(), department.code(), department.name(),
                department.description(), department.createdAt(), department.updatedAt(), department.version());
    }

}
