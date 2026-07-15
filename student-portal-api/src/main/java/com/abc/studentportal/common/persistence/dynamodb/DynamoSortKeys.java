package com.abc.studentportal.common.persistence.dynamodb;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public final class DynamoSortKeys {

    private static final String SEPARATOR = "#";

    private DynamoSortKeys() {

    }

    public static String timestampId(Instant timestamp, UUID id) {

        return timestampPrefix(timestamp) + id;
    }

    public static String timestampPrefix(Instant timestamp) {

        long sortableSeconds = timestamp.getEpochSecond() ^ Long.MIN_VALUE;
        return String.format(Locale.ROOT, "%016x%09d%s", sortableSeconds, timestamp.getNano(), SEPARATOR);
    }

    public static String textId(String text, UUID id) {

        return text.trim().toUpperCase(Locale.ROOT) + SEPARATOR + id;
    }

}
