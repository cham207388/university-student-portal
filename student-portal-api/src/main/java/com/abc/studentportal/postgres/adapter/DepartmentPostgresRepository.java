package com.abc.studentportal.postgres.adapter;

import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.postgres.entity.DepartmentEntity;
import com.abc.studentportal.postgres.repository.DepartmentJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@Profile({"local-postgres", "test-postgres"})
public class DepartmentPostgresRepository implements DepartmentRepository {

    private final DepartmentJpaRepository delegate;

    public DepartmentPostgresRepository(DepartmentJpaRepository delegate) {
        this.delegate = delegate;
    }

    public Department create(Department department) {
        return toDomain(delegate.save(toEntity(department)));
    }

    public Department update(Department department) {
        return toDomain(delegate.save(toEntity(department)));
    }

    public Optional<Department> findById(UUID id) {
        return delegate.findById(id).map(this::toDomain);
    }

    public Optional<Department> findByCode(String code) {
        return delegate.findByCode(code).map(this::toDomain);
    }

    public boolean existsByCode(String code) {
        return delegate.findByCode(code).isPresent();
    }

    public void delete(Department department) {
        delegate.deleteById(department.id());
    }

    private DepartmentEntity toEntity(Department department) {
        return new DepartmentEntity(department.id(), department.code(), department.name(), department.description());
    }

    private Department toDomain(DepartmentEntity departmentEntity) {
        return new Department(departmentEntity.getId(), departmentEntity.getCode(), departmentEntity.getName(), departmentEntity.getDescription(), departmentEntity.getCreatedAt(), departmentEntity.getUpdatedAt(), departmentEntity.getVersion());
    }

}
