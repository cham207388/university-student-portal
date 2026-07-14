package com.abc.studentportal.common.persistence.dynamodb;

import com.abc.studentportal.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamoCursorCodecTest {
	private final DynamoCursorCodec codec = new DynamoCursorCodec();

	@Test
	void roundTripsAKeyAndTreatsMissingCursorAsTheFirstPage() {
		Map<String, AttributeValue> key = Map.of(
				"id", AttributeValue.builder().s("student-1").build(),
				"sequence", AttributeValue.builder().n("42").build());

		String cursor = codec.encode("students-catalog", key);

		assertThat(cursor).doesNotContain("student-1");
		assertThat(codec.decode("students-catalog", cursor)).isEqualTo(key);
		assertThat(codec.decode("students-catalog", null)).isEmpty();
		assertThat(codec.encode("students-catalog", Map.of())).isNull();
	}

	@Test
	void rejectsMalformedAndQueryMismatchedCursors() {
		String cursor = codec.encode("students-catalog",
				Map.of("id", AttributeValue.builder().s("student-1").build()));

		assertThatThrownBy(() -> codec.decode("students-by-status", cursor))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessage("Cursor is invalid or does not belong to this query");
		assertThatThrownBy(() -> codec.decode("students-catalog", "not-base64!"))
				.isInstanceOf(InvalidRequestException.class);
	}
}
