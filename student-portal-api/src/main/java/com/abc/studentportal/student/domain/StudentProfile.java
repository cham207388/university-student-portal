package com.abc.studentportal.student.domain;

import com.abc.studentportal.common.domain.DomainChecks;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudentProfile(UUID id, UUID studentId, LocalDate dateOfBirth, String phoneNumber,
                             String addressLine1, String addressLine2, String city, String state, String postalCode,
                             String country, Instant createdAt, Instant updatedAt, long version) {

    public StudentProfile {

        DomainChecks.audit(id, createdAt, updatedAt, version);
        DomainChecks.required(studentId, "studentId");
        DomainChecks.required(dateOfBirth, "dateOfBirth");
        phoneNumber = DomainChecks.requiredText(phoneNumber, "phoneNumber");
        addressLine1 = DomainChecks.requiredText(addressLine1, "addressLine1");
        addressLine2 = DomainChecks.optionalText(addressLine2);
        city = DomainChecks.requiredText(city, "city");
        state = DomainChecks.requiredText(state, "state");
        postalCode = DomainChecks.requiredText(postalCode, "postalCode");
        country = DomainChecks.requiredText(country, "country");
    }

}
