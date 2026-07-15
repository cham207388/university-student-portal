package com.abc.studentportal.common.persistence.postgres;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

public final class PostgresVersions {

    private PostgresVersions() {

    }

    public static void require(Class<?> type, Object id, long expected, long actual) {

        if (expected != actual) throw new ObjectOptimisticLockingFailureException(type, id);
    }

}
