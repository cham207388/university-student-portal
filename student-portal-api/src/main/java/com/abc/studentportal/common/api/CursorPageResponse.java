package com.abc.studentportal.common.api;

import java.util.List;

public record CursorPageResponse<T>(List<T> content, int limit, String nextCursor, boolean hasNext) {

	public CursorPageResponse {
		content = List.copyOf(content);
	}
}
