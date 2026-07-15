package com.abc.studentportal.student.persistence.postgres;


import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileJpaRepository extends JpaRepository<StudentProfileEntity, UUID> {

    Optional<StudentProfileEntity> findByStudentId(UUID studentId);

}
