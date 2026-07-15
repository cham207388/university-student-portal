package com.abc.studentportal.common.persistence.dynamodb;

import com.abc.studentportal.common.application.DynamoStudentCourseQueries;
import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.persistence.dynamodb.CourseDynamoMapper;
import com.abc.studentportal.course.persistence.dynamodb.CourseDynamoRecord;
import com.abc.studentportal.enrollment.persistence.dynamodb.EnrollmentDynamoRecord;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.persistence.dynamodb.StudentDynamoMapper;
import com.abc.studentportal.student.persistence.dynamodb.StudentDynamoRecord;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@DynamoPersistenceAdapter
public class DynamoStudentCourseQueryService implements DynamoStudentCourseQueries {

    private final DynamoDbClient client;

    private final DynamoDbTables tables;

    private final DynamoCursorCodec cursorCodec;

    public DynamoStudentCourseQueryService(DynamoDbClient client, DynamoDbTables tables,
                                           DynamoCursorCodec cursorCodec) {
        this.client = client;
        this.tables = tables;
        this.cursorCodec = cursorCodec;
    }

    @Override
    public CursorPage<Course> findCoursesByStudent(UUID studentId, CursorRequest request) {
        var edges = edges("enrollment-relationships-by-student", studentId, request);
        List<String> ids = edges.content().stream().map(EnrollmentDynamoRecord::getRelationshipCourseId).toList();
        return new CursorPage<>(batch(ids, tables.courses().tableName(), tables.courses().tableSchema()::mapToItem,
                CourseDynamoMapper::toDomain), edges.limit(), edges.nextCursor(), edges.hasNext());
    }

    @Override
    public CursorPage<Student> findStudentsByCourse(UUID courseId, CursorRequest request) {
        var edges = edges("enrollment-relationships-by-course", courseId, request);
        List<String> ids = edges.content().stream().map(EnrollmentDynamoRecord::getRelationshipStudentId).toList();
        return new CursorPage<>(batch(ids, tables.students().tableName(), tables.students().tableSchema()::mapToItem,
                StudentDynamoMapper::toDomain), edges.limit(), edges.nextCursor(), edges.hasNext());
    }

    private CursorPage<EnrollmentDynamoRecord> edges(String index, UUID ownerId, CursorRequest request) {
        return DynamoCursorQueries.query(tables.enrollments().index(index),
                DynamoCursorQueries.equalTo(ownerId.toString()),
                request, DynamoCursorQueries.identity(tables.enrollments().tableName(), index, ownerId.toString()),
                cursorCodec,
                Function.identity());
    }

    private <R, D> List<D> batch(List<String> ids, String table, Function<Map<String, AttributeValue>, R> recordMapper,
                                 Function<R, D> domainMapper) {
        if (ids.isEmpty())
            return List.of();
        Map<String, KeysAndAttributes> pending = Map.of(table, KeysAndAttributes.builder()
                .keys(ids.stream().map(DynamoStudentCourseQueryService::key).toList()).consistentRead(true).build());
        Map<String, D> found = new LinkedHashMap<>();
        for (int attempt = 0; !pending.isEmpty() && attempt < 3; attempt++) {
            Map<String, KeysAndAttributes> requestItems = pending;
            var response = client.batchGetItem(request -> request.requestItems(requestItems));
            for (Map<String, AttributeValue> item : response.responses().getOrDefault(table, List.of())) {
                R record = recordMapper.apply(item);
                found.put(item.get("id").s(), domainMapper.apply(record));
            }
            pending = response.unprocessedKeys();
        }
        if (!pending.isEmpty())
            throw new ConflictException("Related records could not be read completely; retry the request");
        ArrayList<D> ordered = new ArrayList<>();
        for (String id : ids) {
            D value = found.get(id);
            if (value != null)
                ordered.add(value);
        }
        return List.copyOf(ordered);
    }

    private static Map<String, AttributeValue> key(String id) {
        return Map.of("id", AttributeValue.builder().s(id).build());
    }

}
