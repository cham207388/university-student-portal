package com.abc.studentportal.postgres.repository;

import com.abc.studentportal.postgres.entity.StudentProfileEntity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileJpaRepository extends JpaRepository<StudentProfileEntity, UUID> {

    Optional<StudentProfileEntity> findByStudentId(UUID studentId);

}
