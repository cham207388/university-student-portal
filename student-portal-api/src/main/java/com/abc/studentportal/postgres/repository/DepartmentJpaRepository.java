package com.abc.studentportal.postgres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.abc.studentportal.postgres.entity.DepartmentEntity;

import java.util.Optional;
import java.util.UUID;

public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, UUID> {

    Optional<DepartmentEntity> findByCode(String code);

}
