package com.abc.studentportal.common.migration;

import java.util.UUID;

public interface MigrationTracker {
    UUID start(boolean dryRun, int batchSize);

    void checkpoint(UUID runId, String entityType, long read, long inserted, long skipped, String cursor);

    void complete(UUID runId, boolean withErrors);

    void fail(UUID runId, String entityType, String sourceKey, Exception exception);
}
