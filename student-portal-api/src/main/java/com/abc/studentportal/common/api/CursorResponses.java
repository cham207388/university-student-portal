package com.abc.studentportal.common.api;

import com.abc.studentportal.common.pagination.CursorPage;

import java.util.function.Function;

public final class CursorResponses {

    private CursorResponses() {
    }

    public static <D, R> CursorPageResponse<R> page(CursorPage<D> page, Function<D, R> mapper) {
        return new CursorPageResponse<>(page.content().stream().map(mapper).toList(), page.limit(), page.nextCursor(),
                page.hasNext());
    }

}
