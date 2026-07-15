package com.abc.studentportal.common.domain;

import java.time.Instant;
import java.util.UUID;

public final class DomainChecks {

    private DomainChecks() {

    }

    public static <T> T required(T value, String field) {

        if (value == null) {
            throw new DomainRuleViolationException(field + " is required");
        }
        return value;
    }

    public static String requiredText(String value, String field) {

        if (value == null || value.isBlank()) {
            throw new DomainRuleViolationException(field + " is required");
        }
        return value.trim();
    }

    public static String optionalText(String value) {

        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String uppercaseCode(String value, String field) {

        return requiredText(value, field).toUpperCase(java.util.Locale.ROOT);
    }

    public static long version(long version) {

        if (version < 0) {
            throw new DomainRuleViolationException("version must not be negative");
        }
        return version;
    }

    public static void audit(UUID id, Instant createdAt, Instant updatedAt, long version) {

        required(id, "id");
        required(createdAt, "createdAt");
        required(updatedAt, "updatedAt");
        version(version);
        if (updatedAt.isBefore(createdAt)) {
            throw new DomainRuleViolationException("updatedAt must not be before createdAt");
        }
    }

    public static void positive(int value, String field) {

        if (value <= 0) {
            throw new DomainRuleViolationException(field + " must be positive");
        }
    }

}
