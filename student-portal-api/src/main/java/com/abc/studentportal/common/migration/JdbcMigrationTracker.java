package com.abc.studentportal.common.migration;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Component
@Profile("migration")
public class JdbcMigrationTracker implements MigrationTracker {

    private final JdbcTemplate jdbc;

    public JdbcMigrationTracker(JdbcTemplate jdbc) {

        this.jdbc = jdbc;
    }

    @Override
    public UUID start(boolean dryRun, int batchSize) {

        UUID runId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO migration_runs
                    (id, status, source, target, dry_run, batch_size, started_at)
                VALUES (?, 'STARTED', 'DYNAMODB', 'POSTGRESQL', ?, ?, ?)
                """, runId, dryRun, batchSize, now());
        return runId;
    }

    @Override
    public void checkpoint(UUID runId, String entityType, long read, long inserted, long skipped, String cursor) {

        jdbc.update("""
                INSERT INTO migration_checkpoints
                    (run_id, entity_type, status, read_count, insert_count, skip_count,
                     failure_count, last_processed_key, updated_at)
                VALUES (?, ?, 'COMPLETED', ?, ?, ?, 0, ?, ?)
                ON CONFLICT (run_id, entity_type) DO UPDATE SET
                    status = EXCLUDED.status,
                    read_count = EXCLUDED.read_count,
                    insert_count = EXCLUDED.insert_count,
                    skip_count = EXCLUDED.skip_count,
                    last_processed_key = EXCLUDED.last_processed_key,
                    updated_at = EXCLUDED.updated_at
                """, runId, entityType, read, inserted, skipped, cursor, now());
        jdbc.update("UPDATE migration_runs SET status = 'IN_PROGRESS' WHERE id = ?", runId);
    }

    @Override
    public void complete(UUID runId, boolean withErrors) {

        jdbc.update("UPDATE migration_runs SET status = ?, ended_at = ? WHERE id = ?",
                withErrors ? "COMPLETED_WITH_ERRORS" : "COMPLETED", now(), runId);
    }

    @Override
    public void fail(UUID runId, String entityType, String sourceKey, Exception exception) {

        String message = safeMessage(exception);
        jdbc.update("""
                INSERT INTO migration_errors
                    (run_id, entity_type, source_key, error_type, error_message, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, runId, entityType, sourceKey, exception.getClass().getSimpleName(), message, now());
        jdbc.update("UPDATE migration_runs SET status = 'FAILED', ended_at = ?, error_summary = ? WHERE id = ?",
                now(), message, runId);
    }

    private String safeMessage(Exception exception) {

        String message = exception.getMessage() == null ? "Migration failed" : exception.getMessage();
        return message.substring(0, Math.min(message.length(), 2000));
    }

    private Timestamp now() {

        return Timestamp.from(Instant.now());
    }

}
