package com.abc.studentportal.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Bounded DynamoDB cursor page. DynamoDB does not provide efficient arbitrary offsets or exact totals.")
public record CursorPageResponse<T>(
        @Schema(description = "Items in this page") List<T> content,
        @Schema(description = "Applied page bound", example = "20") int limit,
        @Schema(description = "Opaque query-bound continuation token; null on the final page") String nextCursor,
        @Schema(description = "Whether another page is available", example = "false") boolean hasNext) {

    public CursorPageResponse {
        content = List.copyOf(content);
    }

}
