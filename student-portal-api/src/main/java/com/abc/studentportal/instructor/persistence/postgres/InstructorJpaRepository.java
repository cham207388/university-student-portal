package com.abc.studentportal.instructor.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InstructorJpaRepository extends JpaRepository<InstructorEntity, UUID> {

    boolean existsByDepartment_Id(UUID departmentId);

    Optional<InstructorEntity> findByEmployeeNumber(String employeeNumber);

    Optional<InstructorEntity> findByEmail(String email);
    Page<InstructorEntity> findAll(Pageable pageable);
    Page<InstructorEntity> findByDepartment_Id(UUID departmentId, Pageable pageable);

}
