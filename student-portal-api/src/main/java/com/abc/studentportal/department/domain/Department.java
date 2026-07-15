package com.abc.studentportal.department.domain;

import com.abc.studentportal.common.domain.DomainChecks;

import java.time.Instant;
import java.util.UUID;

public record Department(UUID id, String code, String name, String description,
                         Instant createdAt, Instant updatedAt, long version) {

    public Department {
        DomainChecks.audit(id, createdAt, updatedAt, version);
        code = DomainChecks.uppercaseCode(code, "code");
        name = DomainChecks.requiredText(name, "name");
        description = DomainChecks.optionalText(description);
    }

}
