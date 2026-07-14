package com.abc.studentportal.common.pagination;

import java.util.List;

public record CursorPage<T>(List<T> content, int limit, String nextCursor, boolean hasNext) {
	public CursorPage {
		content = List.copyOf(content);
	}
}
