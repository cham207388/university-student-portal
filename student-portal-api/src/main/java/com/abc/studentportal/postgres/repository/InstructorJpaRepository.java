package com.abc.studentportal.postgres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.abc.studentportal.postgres.entity.InstructorEntity;

import java.util.Optional;
import java.util.UUID;

public interface InstructorJpaRepository extends JpaRepository<InstructorEntity, UUID> {

    Optional<InstructorEntity> findByEmployeeNumber(String employeeNumber);

    Optional<InstructorEntity> findByEmail(String email);

}
