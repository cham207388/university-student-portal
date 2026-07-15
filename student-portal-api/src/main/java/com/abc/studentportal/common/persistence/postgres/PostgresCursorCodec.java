package com.abc.studentportal.common.persistence.postgres;

import com.abc.studentportal.common.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class PostgresCursorCodec {

    public int page(String cursor, String query) {

        if (cursor == null) return 0;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 3 || !"v1".equals(parts[0]) || !query.equals(parts[1])) throw invalid();
            int page = Integer.parseInt(parts[2]);
            if (page < 0) throw invalid();
            return page;
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
    }

    public String next(int page, String query, boolean hasNext) {

        if (!hasNext) return null;
        String value = "v1|" + query + "|" + (page + 1);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private InvalidRequestException invalid() {

        return new InvalidRequestException("cursor is invalid for this PostgreSQL query");
    }

}
