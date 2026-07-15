package com.abc.studentportal.enrollment.api;

import com.abc.studentportal.enrollment.domain.Enrollment;

public final class EnrollmentMapper {

    private EnrollmentMapper() {

    }

    public static EnrollmentApi.Response toResponse(Enrollment enrollment) {

        return new EnrollmentApi.Response(enrollment.id(), enrollment.studentId(), enrollment.courseId(),
                enrollment.status(), enrollment.enrolledAt(), enrollment.droppedAt(), enrollment.finalGrade(),
                enrollment.createdAt(), enrollment.updatedAt(), enrollment.version());
    }

}
