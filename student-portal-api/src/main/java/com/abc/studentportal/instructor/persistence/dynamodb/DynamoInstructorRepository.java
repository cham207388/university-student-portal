package com.abc.studentportal.instructor.persistence.dynamodb;

import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.common.persistence.dynamodb.*;
import com.abc.studentportal.instructor.application.DynamoInstructorQueries;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.domain.Instructor;

import java.util.Optional;
import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoInstructorRepository extends AbstractDynamoRepository<Instructor, InstructorDynamoRecord>
        implements InstructorRepository, DynamoInstructorQueries {

    private final DynamoCursorCodec cursorCodec;

    private final DynamoTransactionalWriter writer;

    private final DynamoRelationshipCounters counters;

    public DynamoInstructorRepository(DynamoDbTables tables, DynamoCursorCodec cursorCodec,
                                      DynamoTransactionalWriter writer, DynamoRelationshipCounters counters) {

        super(tables.instructors(), "id", InstructorDynamoMapper::toRecord, InstructorDynamoMapper::toDomain,
                value -> value.id().toString());
        this.cursorCodec = cursorCodec;
        this.writer = writer;
        this.counters = counters;
    }

    @Override
    public Instructor create(Instructor value) {

        return InstructorDynamoMapper
                .toDomain(writer.create(table(), "id", InstructorDynamoMapper.toRecord(value), claims(value),
                        java.util.List.of(counters.department(value.departmentId().toString(), "instructorCount", 1))));
    }

    @Override
    public Instructor update(Instructor value) {

        InstructorDynamoRecord currentRecord = table()
                .getItem(request -> request.key(key(value.id().toString())).consistentRead(true));
        if (currentRecord == null)
            throw new com.abc.studentportal.common.exception.ConflictException(
                    "Resource does not exist or was modified by another request");
        Instructor current = InstructorDynamoMapper.toDomain(currentRecord);
        InstructorDynamoRecord next = InstructorDynamoMapper.toRecord(value);
        next.setCourseCount(currentRecord.getCourseCount());
        java.util.List<software.amazon.awssdk.services.dynamodb.model.TransactWriteItem> moves = current.departmentId()
                .equals(value.departmentId()) ? java.util.List.of()
                : java.util.List.of(
                counters.department(current.departmentId().toString(), "instructorCount", -1),
                counters.department(value.departmentId().toString(), "instructorCount", 1));
        return InstructorDynamoMapper.toDomain(writer.update(table(), "id", next,
                value.version(), claims(current), claims(value), moves));
    }

    @Override
    public Optional<Instructor> findById(UUID id) {

        return findItem(id.toString());
    }

    @Override
    public boolean existsByEmployeeNumber(String value) {

        return DynamoQueries.exists(table().index("instructors-by-number"), value);
    }

    @Override
    public Optional<Instructor> findByEmployeeNumber(String value) {

        return DynamoQueries.findOne(table().index("instructors-by-number"), value)
                .map(InstructorDynamoMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String value) {

        return DynamoQueries.exists(table().index("instructors-by-email"), value);
    }

    @Override
    public Optional<Instructor> findByEmail(String value) {

        return DynamoQueries.findOne(table().index("instructors-by-email"), value)
                .map(InstructorDynamoMapper::toDomain);
    }

    @Override
    public void delete(Instructor value) {

        Instructor current = findById(value.id())
                .orElseThrow(() -> new com.abc.studentportal.common.exception.ConflictException(
                        "Resource does not exist or was modified by another request"));
        writer.deleteRequiringZeroCounters(table().tableName(), "id", value.id().toString(), value.version(),
                claims(current),
                java.util.List.of(counters.department(current.departmentId().toString(), "instructorCount", -1)),
                java.util.List.of("courseCount"));
    }

    @Override
    public CursorPage<Instructor> findAll(CursorRequest request) {

        return query("instructors-catalog", "INSTRUCTOR", request);
    }

    @Override
    public CursorPage<Instructor> findByDepartment(UUID departmentId, CursorRequest request) {

        return query("instructors-by-department", departmentId.toString(), request);
    }

    private CursorPage<Instructor> query(String index, String partition, CursorRequest request) {

        return DynamoCursorQueries.query(table().index(index), DynamoCursorQueries.equalTo(partition), request,
                DynamoCursorQueries.identity(table().tableName(), index, partition), cursorCodec,
                InstructorDynamoMapper::toDomain);
    }

    private static java.util.List<DynamoUniqueClaim> claims(Instructor value) {

        String owner = value.id().toString();
        return java.util.List.of(new DynamoUniqueClaim("UNIQUE#EMPLOYEE_NUMBER#" + value.employeeNumber(), owner),
                new DynamoUniqueClaim("UNIQUE#EMAIL#" + value.email(), owner));
    }

}
