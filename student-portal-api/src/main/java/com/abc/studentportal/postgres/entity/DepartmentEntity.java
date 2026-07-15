package com.abc.studentportal.postgres.entity;

import jakarta.persistence.*;

import java.util.UUID;

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
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public void updateDetails(String code, String name, String description) {
        this.code = code; this.name = name; this.description = description;
    }

}
