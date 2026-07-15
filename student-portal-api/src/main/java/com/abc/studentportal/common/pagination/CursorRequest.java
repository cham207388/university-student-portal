package com.abc.studentportal.common.pagination;

public record CursorRequest(int limit, String cursor) {

    public CursorRequest {

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
    }

}
