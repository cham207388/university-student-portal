package com.abc.studentportal.department.application;

import com.abc.studentportal.common.application.DependencyChecker;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

@Service
@Profile({"local-postgres", "test-postgres"})
@Transactional
public class PostgresDepartmentService extends DepartmentService {
    public PostgresDepartmentService(DepartmentRepository repository, Clock clock, DependencyChecker dependencies) {
        super(repository, clock, dependencies);
    }
}
