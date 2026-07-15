package com.abc.studentportal.postgres.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "departments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepartmentEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    private String name, description;

    @Version
    private long version;

    public DepartmentEntity(UUID id, String code, String name, String description, Instant c, Instant u) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.createdAt = c;
        this.updatedAt = u;
    }

}
