package com.abc.studentportal.postgres.entity;
import jakarta.persistence.*; import java.util.UUID; @Entity @Table(name="student_profiles") public class StudentProfileEntity { @Id @Column(name="student_id") private UUID studentId; protected StudentProfileEntity(){} public UUID getStudentId(){return studentId;} }
