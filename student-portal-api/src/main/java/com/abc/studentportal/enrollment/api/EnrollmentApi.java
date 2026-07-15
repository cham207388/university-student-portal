package com.abc.studentportal.enrollment.api;

import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class EnrollmentApi {

    private EnrollmentApi() {

    }

    public record CreateRequest(@NotNull UUID studentId, @NotNull UUID courseId) {

    }

    public record StatusRequest(@NotNull EnrollmentStatus status, @Size(max = 20) String finalGrade,
                                @PositiveOrZero long version) {

        @AssertTrue(message = "finalGrade is only valid when status is COMPLETED")
        public boolean isFinalGradeCompatible() {

            return finalGrade == null || finalGrade.isBlank() || status == EnrollmentStatus.COMPLETED;
        }

    }

    public record Response(UUID id, UUID studentId, UUID courseId, EnrollmentStatus status,
                           Instant enrolledAt, Instant droppedAt, String finalGrade,
                           Instant createdAt, Instant updatedAt, long version) {

    }

}
