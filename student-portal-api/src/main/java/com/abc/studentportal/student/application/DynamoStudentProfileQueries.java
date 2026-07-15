package com.abc.studentportal.student.application;

import com.abc.studentportal.student.domain.StudentProfile;

import java.util.Optional;
import java.util.UUID;

public interface DynamoStudentProfileQueries {

    Optional<StudentProfile> findByStudentId(UUID studentId);

}
