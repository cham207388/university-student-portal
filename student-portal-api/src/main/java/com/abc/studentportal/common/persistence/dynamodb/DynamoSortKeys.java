package com.abc.studentportal.common.persistence.dynamodb;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public final class DynamoSortKeys {
	private static final String SEPARATOR = "#";

	private DynamoSortKeys() {
	}

	public static String timestampId(Instant timestamp, UUID id) {
		return timestamp + SEPARATOR + id;
	}

	public static String textId(String text, UUID id) {
		return text.trim().toUpperCase(Locale.ROOT) + SEPARATOR + id;
	}
}
