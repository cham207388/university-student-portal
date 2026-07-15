package com.abc.studentportal.common.persistence.postgres;

import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

public abstract class PostgresPageSupport {
    private final PostgresCursorCodec cursors;

    protected PostgresPageSupport(PostgresCursorCodec cursors) { this.cursors = cursors; }

    protected <E, D> CursorPage<D> page(String query, CursorRequest request,
            Function<org.springframework.data.domain.Pageable, Page<E>> loader, Function<E, D> mapper) {
        int number = cursors.page(request.cursor(), query);
        Page<E> result = loader.apply(PageRequest.of(number, request.limit(), Sort.by("id").ascending()));
        return new CursorPage<>(result.getContent().stream().map(mapper).toList(), request.limit(),
                cursors.next(number, query, result.hasNext()), result.hasNext());
    }
}
