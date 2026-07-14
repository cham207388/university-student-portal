package com.abc.studentportal.common.pagination;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationRequestTest {

	@Test
	void enforcesBoundedCursorAndPageRequests() {
		assertThatThrownBy(() -> new CursorRequest(0, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("limit must be between 1 and 100");
		assertThatThrownBy(() -> new PageRequest(-1, 20, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("page must not be negative");
		assertThatThrownBy(() -> new PageRequest(0, 101, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("size must be between 1 and 100");
	}
}
