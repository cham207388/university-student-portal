package com.abc.studentportal.student.persistence.postgres;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentProfileJpaRepository extends JpaRepository<StudentProfileEntity, UUID> {

    Optional<StudentProfileEntity> findByStudentId(UUID studentId);

}
