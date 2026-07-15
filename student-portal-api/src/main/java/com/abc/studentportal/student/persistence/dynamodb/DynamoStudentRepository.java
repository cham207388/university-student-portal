package com.abc.studentportal.student.persistence.dynamodb;

import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.common.persistence.dynamodb.*;
import com.abc.studentportal.student.application.DynamoStudentQueries;
import com.abc.studentportal.student.application.StudentRepository;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoStudentRepository extends AbstractDynamoRepository<Student, StudentDynamoRecord>
        implements StudentRepository, DynamoStudentQueries {

    private final DynamoCursorCodec cursorCodec;

    private final DynamoTransactionalWriter writer;

    private final software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable<StudentProfileDynamoRecord> profiles;

    private final DynamoRelationshipCounters counters;

    public DynamoStudentRepository(DynamoDbTables tables, DynamoCursorCodec cursorCodec,
                                   DynamoTransactionalWriter writer, DynamoRelationshipCounters counters) {

        super(tables.students(), "id", StudentDynamoMapper::toRecord, StudentDynamoMapper::toDomain,
                value -> value.id().toString());
        this.cursorCodec = cursorCodec;
        this.writer = writer;
        this.profiles = tables.studentProfiles();
        this.counters = counters;
    }

    @Override
    public Student create(Student value) {

        return StudentDynamoMapper
                .toDomain(writer.create(table(), "id", StudentDynamoMapper.toRecord(value), claims(value),
                        List.of(counters.department(value.departmentId().toString(), "studentCount", 1))));
    }

    @Override
    public Student update(Student value) {

        StudentDynamoRecord currentRecord = table()
                .getItem(request -> request.key(key(value.id().toString())).consistentRead(true));
        if (currentRecord == null)
            throw new com.abc.studentportal.common.exception.ConflictException(
                    "Resource does not exist or was modified by another request");
        Student current = StudentDynamoMapper.toDomain(currentRecord);
        StudentDynamoRecord next = StudentDynamoMapper.toRecord(value);
        next.setEnrollmentCount(currentRecord.getEnrollmentCount());
        List<software.amazon.awssdk.services.dynamodb.model.TransactWriteItem> moves = current.departmentId()
                .equals(value.departmentId()) ? List.of()
                : List.of(
                counters.department(current.departmentId().toString(), "studentCount", -1),
                counters.department(value.departmentId().toString(), "studentCount", 1));
        return StudentDynamoMapper.toDomain(writer.update(table(), "id", next, value.version(),
                claims(current), claims(value), moves));
    }

    @Override
    public Optional<Student> findById(UUID id) {

        return findItem(id.toString());
    }

    @Override
    public boolean existsByStudentNumber(String value) {

        return DynamoQueries.exists(table().index("students-by-number"), value);
    }

    @Override
    public Optional<Student> findByStudentNumber(String value) {

        return DynamoQueries.findOne(table().index("students-by-number"), value).map(StudentDynamoMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String value) {

        return DynamoQueries.exists(table().index("students-by-email"), value);
    }

    @Override
    public Optional<Student> findByEmail(String value) {

        return DynamoQueries.findOne(table().index("students-by-email"), value).map(StudentDynamoMapper::toDomain);
    }

    @Override
    public void delete(Student value) {

        Student current = findById(value.id())
                .orElseThrow(() -> new com.abc.studentportal.common.exception.ConflictException(
                        "Resource does not exist or was modified by another request"));
        StudentProfileDynamoRecord profile = profiles
                .getItem(request -> request.key(key(value.id().toString())).consistentRead(true));
        List<software.amazon.awssdk.services.dynamodb.model.TransactWriteItem> extra = profile == null ? List.of()
                : List.of(software.amazon.awssdk.services.dynamodb.model.TransactWriteItem.builder().delete(
                        software.amazon.awssdk.services.dynamodb.model.Delete.builder().tableName(profiles.tableName())
                                .key(java.util.Map.of("studentId",
                                        software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder()
                                                .s(value.id().toString()).build()))
                                .conditionExpression("#version = :version")
                                .expressionAttributeNames(java.util.Map.of("#version", "version"))
                                .expressionAttributeValues(java.util.Map.of(":version",
                                        software.amazon.awssdk.services.dynamodb.model.AttributeValue
                                                .builder().n(profile.getVersion().toString()).build()))
                                .build())
                .build());
        java.util.ArrayList<software.amazon.awssdk.services.dynamodb.model.TransactWriteItem> actions = new java.util.ArrayList<>(
                extra);
        actions.add(counters.department(current.departmentId().toString(), "studentCount", -1));
        writer.delete(table().tableName(), "id", value.id().toString(), value.version(), claims(current), actions,
                true);
    }

    @Override
    public CursorPage<Student> findAll(CursorRequest request) {

        return query("students-catalog", DynamoCursorQueries.equalTo("STUDENT"), request, "STUDENT");
    }

    @Override
    public CursorPage<Student> findByDepartment(UUID departmentId, String lastNamePrefix, CursorRequest request) {

        String partition = departmentId.toString();
        String prefix = lastNamePrefix == null || lastNamePrefix.isBlank() ? null
                : lastNamePrefix.trim().toUpperCase(java.util.Locale.ROOT);
        var condition = prefix == null ? DynamoCursorQueries.equalTo(partition)
                : DynamoCursorQueries.beginsWith(partition, prefix);
        return query("students-by-department", condition, request, partition, prefix);
    }

    @Override
    public CursorPage<Student> findByStatus(StudentStatus status, CursorRequest request) {

        return query("students-by-status", DynamoCursorQueries.equalTo(status.name()), request, status.name());
    }

    private CursorPage<Student> query(String index,
                                      software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional condition,
                                      CursorRequest request, String... parameters) {

        return DynamoCursorQueries.query(table().index(index), condition, request,
                DynamoCursorQueries.identity(table().tableName(), index, parameters), cursorCodec,
                StudentDynamoMapper::toDomain);
    }

    private static java.util.List<DynamoUniqueClaim> claims(Student value) {

        String owner = value.id().toString();
        return java.util.List.of(new DynamoUniqueClaim("UNIQUE#STUDENT_NUMBER#" + value.studentNumber(), owner),
                new DynamoUniqueClaim("UNIQUE#EMAIL#" + value.email(), owner));
    }

}
