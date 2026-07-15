package com.abc.studentportal.postgres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.abc.studentportal.postgres.entity.StudentEntity;

import java.util.Optional;
import java.util.UUID;

public interface StudentJpaRepository extends JpaRepository<StudentEntity, UUID> {

    boolean existsByDepartment_Id(UUID departmentId);

    Optional<StudentEntity> findByStudentNumber(String studentNumber);

    Optional<StudentEntity> findByEmail(String email);

}
