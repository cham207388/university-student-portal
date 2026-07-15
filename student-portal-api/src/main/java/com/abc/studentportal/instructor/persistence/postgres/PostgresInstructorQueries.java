package com.abc.studentportal.instructor.persistence.postgres;

import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.common.persistence.postgres.PostgresCursorCodec;
import com.abc.studentportal.common.persistence.postgres.PostgresDomainMapper;
import com.abc.studentportal.common.persistence.postgres.PostgresPageSupport;
import com.abc.studentportal.instructor.application.InstructorQueries;
import com.abc.studentportal.instructor.domain.Instructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Profile({"local-postgres", "test-postgres"})
@Transactional(readOnly = true)
public class PostgresInstructorQueries extends PostgresPageSupport implements InstructorQueries {

    private final InstructorJpaRepository repository;

    public PostgresInstructorQueries(InstructorJpaRepository repository, PostgresCursorCodec cursors) {

        super(cursors);
        this.repository = repository;
    }

    public CursorPage<Instructor> findAll(CursorRequest request) {

        return page("instructors", request, repository::findAll, PostgresDomainMapper::instructor);
    }

    public CursorPage<Instructor> findByDepartment(UUID id, CursorRequest request) {

        return page("instructors:department:" + id, request, p -> repository.findByDepartment_Id(id, p), PostgresDomainMapper::instructor);
    }

}
