package com.abc.studentportal.student.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.abc.studentportal.student.domain.StudentStatus;

public interface StudentJpaRepository extends JpaRepository<StudentEntity, UUID> {

    boolean existsByDepartment_Id(UUID departmentId);

    Optional<StudentEntity> findByStudentNumber(String studentNumber);

    Optional<StudentEntity> findByEmail(String email);
    Page<StudentEntity> findAll(Pageable pageable);
    Page<StudentEntity> findByDepartment_Id(UUID departmentId, Pageable pageable);
    Page<StudentEntity> findByDepartment_IdAndLastNameStartingWithIgnoreCase(UUID departmentId, String lastName, Pageable pageable);
    Page<StudentEntity> findByStatus(StudentStatus status, Pageable pageable);

}
