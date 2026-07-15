package com.abc.studentportal.instructor.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class InstructorApi {

    private InstructorApi() {
    }

    public record CreateRequest(@NotBlank @Size(max = 50) String employeeNumber,
                                @NotBlank @Size(max = 100) String firstName, @NotBlank @Size(max = 100) String lastName,
                                @NotBlank @Email @Size(max = 320) String email, @NotNull UUID departmentId) {

    }

    public record UpdateRequest(@NotBlank @Size(max = 50) String employeeNumber,
                                @NotBlank @Size(max = 100) String firstName, @NotBlank @Size(max = 100) String lastName,
                                @NotBlank @Email @Size(max = 320) String email, @NotNull UUID departmentId,
                                @PositiveOrZero long version) {

    }

    public record Response(UUID id, String employeeNumber, String firstName, String lastName, String email,
                           UUID departmentId, Instant createdAt, Instant updatedAt, long version) {

    }

}
