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
  public DepartmentPostgresRepository(DepartmentJpaRepository delegate) { this.delegate = delegate; }
  public Department create(Department d) { return toDomain(delegate.save(toEntity(d))); }
  public Department update(Department d) { return toDomain(delegate.save(toEntity(d))); }
  public Optional<Department> findById(UUID id) { return delegate.findById(id).map(this::toDomain); }
  public Optional<Department> findByCode(String code) { return delegate.findByCode(code).map(this::toDomain); }
  public boolean existsByCode(String code) { return delegate.findByCode(code).isPresent(); }
  public void delete(Department d) { delegate.deleteById(d.id()); }
  private DepartmentEntity toEntity(Department d) { return new DepartmentEntity(d.id(), d.code(), d.name(), d.description(), d.createdAt(), d.updatedAt()); }
  private Department toDomain(DepartmentEntity e) { return new Department(e.getId(), e.getCode(), e.getName(), e.getDescription(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion()); }
}
