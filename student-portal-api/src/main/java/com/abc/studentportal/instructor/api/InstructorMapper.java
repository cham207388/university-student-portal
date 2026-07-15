package com.abc.studentportal.instructor.api;

import com.abc.studentportal.instructor.domain.Instructor;

public final class InstructorMapper {

    private InstructorMapper() {

    }

    public static InstructorApi.Response toResponse(Instructor instructor) {

        return new InstructorApi.Response(instructor.id(), instructor.employeeNumber(), instructor.firstName(),
                instructor.lastName(), instructor.email(), instructor.departmentId(), instructor.createdAt(),
                instructor.updatedAt(), instructor.version());
    }

}
