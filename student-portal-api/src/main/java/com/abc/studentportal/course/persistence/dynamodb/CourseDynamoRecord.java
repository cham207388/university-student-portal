package com.abc.studentportal.course.persistence.dynamodb;

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
public class CourseDynamoRecord implements com.abc.studentportal.common.persistence.dynamodb.VersionedDynamoRecord {

    private String id;

    private String courseCode;

    private String title;

    private String description;

    private Integer credits;

    private Integer capacity;

    private Long occupiedSeats;

    private Long enrollmentCount;

    private String status;

    private String departmentId;

    private String instructorId;

    private String entityType;

    private String createdAt;

    private String updatedAt;

    private String createdAtId;

    private String updatedAtId;

    private String courseCodeId;

    private Long version;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "courses-by-code")
    public String getCourseCode() {
        return courseCode;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "courses-by-department")
    public String getDepartmentId() {
        return departmentId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "courses-by-instructor")
    public String getInstructorId() {
        return instructorId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "courses-by-status")
    public String getStatus() {
        return status;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "courses-catalog")
    public String getEntityType() {
        return entityType;
    }

    @DynamoDbSecondarySortKey(indexNames = "courses-catalog")
    public String getCreatedAtId() {
        return createdAtId;
    }

    @DynamoDbSecondarySortKey(indexNames = "courses-by-status")
    public String getUpdatedAtId() {
        return updatedAtId;
    }

    @DynamoDbSecondarySortKey(indexNames = {"courses-by-department", "courses-by-instructor"})
    public String getCourseCodeId() {
        return courseCodeId;
    }

    @DynamoDbVersionAttribute
    public Long getVersion() {
        return version;
    }

}
