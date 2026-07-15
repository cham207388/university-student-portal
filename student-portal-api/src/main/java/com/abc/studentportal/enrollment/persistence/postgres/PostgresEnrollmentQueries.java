package com.abc.studentportal.enrollment.persistence.postgres;

import com.abc.studentportal.common.pagination.*;
import com.abc.studentportal.enrollment.application.EnrollmentQueries;
import com.abc.studentportal.enrollment.domain.*;
import com.abc.studentportal.common.persistence.postgres.PostgresCursorCodec;
import com.abc.studentportal.common.persistence.postgres.PostgresDomainMapper;
import com.abc.studentportal.common.persistence.postgres.PostgresPageSupport;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Component @Profile({"local-postgres", "test-postgres"}) @Transactional(readOnly = true)
public class PostgresEnrollmentQueries extends PostgresPageSupport implements EnrollmentQueries {
    private final EnrollmentJpaRepository repository;
    public PostgresEnrollmentQueries(EnrollmentJpaRepository repository, PostgresCursorCodec cursors) { super(cursors); this.repository = repository; }
    public CursorPage<Enrollment> findAll(CursorRequest request) { return page("enrollments", request, repository::findAll, PostgresDomainMapper::enrollment); }
    public CursorPage<Enrollment> findByStudent(UUID id, Instant from, Instant to, CursorRequest request) {
        return page("enrollments:student:" + id + ":" + from + ":" + to, request,
                p -> from != null && to != null ? repository.findByStudent_IdAndEnrolledAtBetween(id, from, to, p)
                        : from != null ? repository.findByStudent_IdAndEnrolledAtGreaterThanEqual(id, from, p)
                        : to != null ? repository.findByStudent_IdAndEnrolledAtLessThanEqual(id, to, p)
                        : repository.findByStudent_Id(id, p), PostgresDomainMapper::enrollment);
    }
    public CursorPage<Enrollment> findByCourse(UUID id, Instant from, Instant to, CursorRequest request) {
        return page("enrollments:course:" + id + ":" + from + ":" + to, request,
                p -> from != null && to != null ? repository.findByCourse_IdAndEnrolledAtBetween(id, from, to, p)
                        : from != null ? repository.findByCourse_IdAndEnrolledAtGreaterThanEqual(id, from, p)
                        : to != null ? repository.findByCourse_IdAndEnrolledAtLessThanEqual(id, to, p)
                        : repository.findByCourse_Id(id, p), PostgresDomainMapper::enrollment);
    }
    public CursorPage<Enrollment> findByStatus(EnrollmentStatus status, CursorRequest request) { return page("enrollments:status:" + status, request, p -> repository.findByStatus(status, p), PostgresDomainMapper::enrollment); }
}
