package com.abc.studentportal.enrollment.persistence.postgres;
import com.abc.studentportal.enrollment.application.EnrollmentRepository; import com.abc.studentportal.enrollment.domain.*; import com.abc.studentportal.postgres.entity.*; import com.abc.studentportal.postgres.repository.*; import java.util.*; import org.springframework.stereotype.Repository; import org.springframework.context.annotation.Profile; import org.springframework.transaction.annotation.Transactional;
@Repository @Profile({"local-postgres","test-postgres"}) @Transactional public class PostgresEnrollmentRepository implements EnrollmentRepository {
 private final EnrollmentJpaRepository repo; private final StudentJpaRepository students; private final CourseJpaRepository courses;
 public PostgresEnrollmentRepository(EnrollmentJpaRepository repo, StudentJpaRepository students, CourseJpaRepository courses){this.repo=repo;this.students=students;this.courses=courses;}
 public Enrollment create(Enrollment e){return toDomain(repo.save(toEntity(e)));} public Enrollment update(Enrollment e){return toDomain(repo.save(toEntity(e)));} public Optional<Enrollment> findById(UUID id){return repo.findById(id).map(this::toDomain);}
 public boolean existsActiveByStudentIdAndCourseId(UUID s,UUID c){return repo.existsByStudent_IdAndCourse_IdAndStatusIn(s,c,Set.of(EnrollmentStatus.ENROLLED,EnrollmentStatus.WAITLISTED));}
 public boolean existsByStudentId(UUID id){return repo.existsByStudent_Id(id);} public boolean existsByCourseId(UUID id){return repo.existsByCourse_Id(id);}
 private EnrollmentEntity toEntity(Enrollment e){return new EnrollmentEntity(e.id(),students.getReferenceById(e.studentId()),courses.getReferenceById(e.courseId()),e.status(),e.enrolledAt(),e.droppedAt(),e.finalGrade(),e.createdAt(),e.updatedAt(),e.version());}
 private Enrollment toDomain(EnrollmentEntity e){return new Enrollment(e.getId(),e.getStudentId(),e.getCourseId(),e.getStatus(),e.getEnrolledAt(),e.getDroppedAt(),e.getFinalGrade(),e.getCreatedAt(),e.getUpdatedAt(),e.getVersion());}
}
