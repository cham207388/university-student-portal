package com.abc.studentportal.student.api;

import com.abc.studentportal.student.domain.StudentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class StudentApi {

    private StudentApi() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 50) String studentNumber,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 320) String email,
            @NotNull StudentStatus status,
            @NotNull UUID departmentId) {

    }

    public record UpdateRequest(
            @NotBlank @Size(max = 50) String studentNumber,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 320) String email,
            @NotNull UUID departmentId,
            @PositiveOrZero long version) {

    }

    public record StatusRequest(@NotNull StudentStatus status, @PositiveOrZero long version) {

    }

    public record ProfileRequest(
            @Past @NotNull LocalDate dateOfBirth,
            @NotBlank @Size(max = 40) String phoneNumber,
            @NotBlank @Size(max = 200) String addressLine1,
            @Size(max = 200) String addressLine2,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Size(max = 30) String postalCode,
            @NotBlank @Size(max = 100) String country,
            @PositiveOrZero long version) {

    }

    public record Response(UUID id, String studentNumber, String firstName, String lastName, String email,
                           StudentStatus status, UUID departmentId, Instant createdAt, Instant updatedAt,
                           long version) {

    }

    public record ProfileResponse(UUID id, UUID studentId, LocalDate dateOfBirth, String phoneNumber,
                                  String addressLine1, String addressLine2, String city, String state,
                                  String postalCode,
                                  String country, Instant createdAt, Instant updatedAt, long version) {

    }

}
