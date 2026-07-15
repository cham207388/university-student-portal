package com.abc.studentportal.postgres.adapter;

import com.abc.studentportal.common.pagination.*;
import com.abc.studentportal.postgres.repository.StudentJpaRepository;
import com.abc.studentportal.student.application.StudentQueries;
import com.abc.studentportal.student.domain.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Component @Profile({"local-postgres", "test-postgres"}) @Transactional(readOnly = true)
public class PostgresStudentQueries extends PostgresPageSupport implements StudentQueries {
    private final StudentJpaRepository repository;
    public PostgresStudentQueries(StudentJpaRepository repository, PostgresCursorCodec cursors) { super(cursors); this.repository = repository; }
    public CursorPage<Student> findAll(CursorRequest request) { return page("students", request, repository::findAll, PostgresDomainMapper::student); }
    public CursorPage<Student> findByDepartment(UUID id, String prefix, CursorRequest request) {
        String query = "students:department:" + id + ":" + (prefix == null ? "" : prefix.toLowerCase());
        return page(query, request, p -> prefix == null ? repository.findByDepartment_Id(id, p)
                : repository.findByDepartment_IdAndLastNameStartingWithIgnoreCase(id, prefix, p), PostgresDomainMapper::student);
    }
    public CursorPage<Student> findByStatus(StudentStatus status, CursorRequest request) {
        return page("students:status:" + status, request, p -> repository.findByStatus(status, p), PostgresDomainMapper::student);
    }
}
