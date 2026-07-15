package com.abc.studentportal.instructor.domain;

import com.abc.studentportal.common.domain.DomainChecks;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record Instructor(UUID id, String employeeNumber, String firstName, String lastName, String email,
                         UUID departmentId, Instant createdAt, Instant updatedAt, long version) {

    public Instructor {

        DomainChecks.audit(id, createdAt, updatedAt, version);
        employeeNumber = DomainChecks.requiredText(employeeNumber, "employeeNumber");
        firstName = DomainChecks.requiredText(firstName, "firstName");
        lastName = DomainChecks.requiredText(lastName, "lastName");
        email = DomainChecks.requiredText(email, "email").toLowerCase(Locale.ROOT);
        DomainChecks.required(departmentId, "departmentId");
    }

}
