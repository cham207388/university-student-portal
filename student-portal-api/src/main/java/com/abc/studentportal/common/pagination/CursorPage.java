package com.abc.studentportal.common.pagination;

import java.util.List;
import java.util.Optional;

public record CursorPage<T>(List<T> content, int limit, String nextCursor, boolean hasNext) {

    public CursorPage {

        content = List.copyOf(content);
    }

    /** Exact alternate-key lookup page: limit 1, no continuation (empty content is a valid miss). */
    public static <T> CursorPage<T> exact(Optional<T> value) {
        return new CursorPage<>(value.stream().toList(), 1, null, false);
    }

}
