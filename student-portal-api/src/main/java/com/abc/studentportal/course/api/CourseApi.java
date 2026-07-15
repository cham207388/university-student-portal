package com.abc.studentportal.course.api;

import com.abc.studentportal.course.domain.CourseStatus;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public final class CourseApi {

    private CourseApi() {

    }

    public record CreateRequest(@NotBlank @Size(max = 30) String courseCode,
                                @NotBlank @Size(max = 200) String title, @Size(max = 4000) String description,
                                @Positive int credits, @Positive int capacity, @NotNull CourseStatus status,
                                @NotNull UUID departmentId, @NotNull UUID instructorId) {

    }

    public record UpdateRequest(@NotBlank @Size(max = 30) String courseCode,
                                @NotBlank @Size(max = 200) String title, @Size(max = 4000) String description,
                                @Positive int credits, @Positive int capacity, @NotNull UUID departmentId,
                                @NotNull UUID instructorId, @PositiveOrZero long version) {

    }

    public record StatusRequest(@NotNull CourseStatus status, @PositiveOrZero long version) {

    }

    public record Response(UUID id, String courseCode, String title, String description, int credits,
                           int capacity, CourseStatus status, UUID departmentId, UUID instructorId,
                           Instant createdAt, Instant updatedAt, long version) {

    }

}
