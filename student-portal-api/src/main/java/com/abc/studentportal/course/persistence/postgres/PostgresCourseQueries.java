package com.abc.studentportal.course.persistence.postgres;

import com.abc.studentportal.common.pagination.*;
import com.abc.studentportal.course.application.CourseQueries;
import com.abc.studentportal.course.domain.*;
import com.abc.studentportal.common.persistence.postgres.PostgresCursorCodec;
import com.abc.studentportal.common.persistence.postgres.PostgresDomainMapper;
import com.abc.studentportal.common.persistence.postgres.PostgresPageSupport;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Component @Profile({"local-postgres", "test-postgres"}) @Transactional(readOnly = true)
public class PostgresCourseQueries extends PostgresPageSupport implements CourseQueries {
    private final CourseJpaRepository repository;
    public PostgresCourseQueries(CourseJpaRepository repository, PostgresCursorCodec cursors) { super(cursors); this.repository = repository; }
    public CursorPage<Course> findAll(CursorRequest request) { return page("courses", request, repository::findAll, PostgresDomainMapper::course); }
    public CursorPage<Course> findByDepartment(UUID id, CursorRequest request) { return page("courses:department:" + id, request, p -> repository.findByDepartment_Id(id, p), PostgresDomainMapper::course); }
    public CursorPage<Course> findByInstructor(UUID id, CursorRequest request) { return page("courses:instructor:" + id, request, p -> repository.findByInstructor_Id(id, p), PostgresDomainMapper::course); }
    public CursorPage<Course> findByStatus(CourseStatus status, CursorRequest request) { return page("courses:status:" + status, request, p -> repository.findByStatus(status, p), PostgresDomainMapper::course); }
}
