package com.abc.studentportal.common.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties("student-portal.dynamodb")
public record DynamoDbProperties(
        @NotBlank String region,
        @NotNull URI endpoint,
        @NotNull @Valid Tables tables) {

    public record Tables(
            @NotBlank String departments,
            @NotBlank String students,
            @NotBlank String studentProfiles,
            @NotBlank String instructors,
            @NotBlank String courses,
            @NotBlank String enrollments) {

    }

}
