package com.abc.studentportal.department.persistence.dynamodb;

import com.abc.studentportal.common.persistence.dynamodb.DynamoSortKeys;
import com.abc.studentportal.department.domain.Department;

import java.time.Instant;
import java.util.UUID;

public final class DepartmentDynamoMapper {

    private DepartmentDynamoMapper() {
    }

    public static DepartmentDynamoRecord toRecord(Department value) {
        DepartmentDynamoRecord record = new DepartmentDynamoRecord();
        record.setId(value.id().toString());
        record.setCode(value.code());
        record.setName(value.name());
        record.setDescription(value.description());
        record.setEntityType("DEPARTMENT");
        record.setCreatedAt(value.createdAt().toString());
        record.setUpdatedAt(value.updatedAt().toString());
        record.setCreatedAtId(DynamoSortKeys.timestampId(value.createdAt(), value.id()));
        record.setStudentCount(0L);
        record.setInstructorCount(0L);
        record.setCourseCount(0L);
        record.setVersion(value.version());
        return record;
    }

    public static Department toDomain(DepartmentDynamoRecord value) {
        return new Department(UUID.fromString(value.getId()), value.getCode(), value.getName(), value.getDescription(),
                Instant.parse(value.getCreatedAt()), Instant.parse(value.getUpdatedAt()), value.getVersion());
    }

}
