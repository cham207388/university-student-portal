package com.abc.studentportal.student.persistence.dynamodb;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
public class StudentDynamoRecord implements com.abc.studentportal.common.persistence.dynamodb.VersionedDynamoRecord {

    private String id;

    private String studentNumber;

    private String firstName;

    private String lastName;

    private String email;

    private String status;

    private String departmentId;

    private String entityType;

    private String createdAt;

    private String updatedAt;

    private String createdAtId;

    private String updatedAtId;

    private String lastNameId;

    private Long enrollmentCount;

    private Long version;

    @DynamoDbPartitionKey
    public String getId() {

        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "students-by-number")
    public String getStudentNumber() {

        return studentNumber;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "students-by-email")
    public String getEmail() {

        return email;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "students-by-department")
    public String getDepartmentId() {

        return departmentId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "students-by-status")
    public String getStatus() {

        return status;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "students-catalog")
    public String getEntityType() {

        return entityType;
    }

    @DynamoDbSecondarySortKey(indexNames = "students-catalog")
    public String getCreatedAtId() {

        return createdAtId;
    }

    @DynamoDbSecondarySortKey(indexNames = "students-by-status")
    public String getUpdatedAtId() {

        return updatedAtId;
    }

    @DynamoDbSecondarySortKey(indexNames = "students-by-department")
    public String getLastNameId() {

        return lastNameId;
    }

    @DynamoDbVersionAttribute
    public Long getVersion() {

        return version;
    }

}
