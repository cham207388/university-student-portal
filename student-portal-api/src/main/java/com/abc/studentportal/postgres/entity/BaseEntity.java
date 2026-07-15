package com.abc.studentportal.postgres.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Common identity and lifecycle columns for all PostgreSQL entities. */
@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {
    @Id
    protected UUID id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    protected Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    protected Instant updatedAt;

}
