package com.abc.studentportal.postgres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.abc.studentportal.postgres.entity.StudentEntity;

import java.util.*;

public interface StudentJpaRepository extends JpaRepository<StudentEntity, UUID> {

    Optional<StudentEntity> findByStudentNumber(String studentNumber);

    Optional<StudentEntity> findByEmail(String email);

}
