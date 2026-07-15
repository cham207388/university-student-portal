package com.abc.studentportal.postgres.entity;

import jakarta.persistence.*;

import java.util.UUID;
import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "departments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepartmentEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    private String name, description;

    @Version
    private long version;

    public DepartmentEntity(UUID id, String code, String name, String description) {
        this(id, code, name, description, null, null, 0);
    }

    public DepartmentEntity(UUID id, String code, String name, String description, Instant createdAt, Instant updatedAt, long version) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.version = version;
        audit(createdAt, updatedAt);
    }

    public void updateDetails(String code, String name, String description) {
        this.code = code; this.name = name; this.description = description;
    }

}
