package com.abc.studentportal.department.persistence.postgres;

import com.abc.studentportal.common.pagination.*;
import com.abc.studentportal.department.application.DepartmentQueries;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.common.persistence.postgres.PostgresCursorCodec;
import com.abc.studentportal.common.persistence.postgres.PostgresDomainMapper;
import com.abc.studentportal.common.persistence.postgres.PostgresPageSupport;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @Profile({"local-postgres", "test-postgres"}) @Transactional(readOnly = true)
public class PostgresDepartmentQueries extends PostgresPageSupport implements DepartmentQueries {
    private final DepartmentJpaRepository repository;
    public PostgresDepartmentQueries(DepartmentJpaRepository repository, PostgresCursorCodec cursors) { super(cursors); this.repository = repository; }
    public CursorPage<Department> findAll(CursorRequest request) { return page("departments", request, repository::findAll, PostgresDomainMapper::department); }
}
