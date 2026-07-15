package com.abc.studentportal.postgres.adapter;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

final class PostgresVersions {
    private PostgresVersions() { }
    static void require(Class<?> type, Object id, long expected, long actual) {
        if (expected != actual) throw new ObjectOptimisticLockingFailureException(type, id);
    }
}
