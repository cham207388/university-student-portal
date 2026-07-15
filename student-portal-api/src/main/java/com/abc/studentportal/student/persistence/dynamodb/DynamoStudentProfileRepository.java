package com.abc.studentportal.student.persistence.dynamodb;

import com.abc.studentportal.common.persistence.dynamodb.AbstractDynamoRepository;
import com.abc.studentportal.common.persistence.dynamodb.DynamoDbTables;
import com.abc.studentportal.common.persistence.dynamodb.DynamoPersistenceAdapter;
import com.abc.studentportal.student.application.StudentProfileRepository;
import com.abc.studentportal.student.domain.StudentProfile;

import java.util.Optional;
import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoStudentProfileRepository extends AbstractDynamoRepository<StudentProfile, StudentProfileDynamoRecord>
        implements StudentProfileRepository, com.abc.studentportal.student.application.DynamoStudentProfileQueries {

    public DynamoStudentProfileRepository(DynamoDbTables tables) {

        super(tables.studentProfiles(), "studentId", StudentProfileDynamoMapper::toRecord,
                StudentProfileDynamoMapper::toDomain,
                value -> value.studentId().toString());
    }

    @Override
    public StudentProfile create(StudentProfile value) {

        return createItem(value);
    }

    @Override
    public StudentProfile update(StudentProfile value) {

        return updateItem(value);
    }

    @Override
    public Optional<StudentProfile> findByStudentId(UUID id) {

        return findItem(id.toString());
    }

    @Override
    public void delete(StudentProfile value) {

        deleteItem(value, value.version());
    }

}
