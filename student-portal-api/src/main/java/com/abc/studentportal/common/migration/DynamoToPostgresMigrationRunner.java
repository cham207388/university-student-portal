package com.abc.studentportal.common.migration;

import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.department.application.DynamoDepartmentQueries;
import com.abc.studentportal.instructor.application.DynamoInstructorQueries;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.course.application.DynamoCourseQueries;
import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.enrollment.application.DynamoEnrollmentQueries;
import com.abc.studentportal.enrollment.application.EnrollmentRepository;
import com.abc.studentportal.student.application.DynamoStudentQueries;
import com.abc.studentportal.student.application.StudentRepository;
import com.abc.studentportal.student.application.StudentProfileRepository;
import com.abc.studentportal.student.application.DynamoStudentProfileQueries;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.UUID;

@Component
@Profile("migration")
@ConditionalOnProperty(name = "migration.run", havingValue = "true")
public class DynamoToPostgresMigrationRunner implements ApplicationRunner {
    private final DynamoDepartmentQueries dynamoDepartments;
    private final DynamoInstructorQueries dynamoInstructors;
    private final DepartmentRepository departments;
    private final InstructorRepository instructors;
    private final ConfigurableApplicationContext context;
    private final DynamoStudentQueries dynamoStudents;
    private final StudentRepository students;
    private final StudentProfileRepository profiles;
    private final DynamoStudentProfileQueries dynamoProfiles;
    private final DynamoCourseQueries dynamoCourses;
    private final CourseRepository courses;
    private final DynamoEnrollmentQueries dynamoEnrollments;
    private final EnrollmentRepository enrollments;
    private final int batchSize;
    private final boolean dryRun;
    private final MigrationTracker tracker;

    public DynamoToPostgresMigrationRunner(DynamoDepartmentQueries dynamoDepartments,
            DynamoInstructorQueries dynamoInstructors, DepartmentRepository departments,
            InstructorRepository instructors, ConfigurableApplicationContext context,
            DynamoStudentQueries dynamoStudents, StudentRepository students,
            StudentProfileRepository profiles, DynamoStudentProfileQueries dynamoProfiles,
            DynamoCourseQueries dynamoCourses, CourseRepository courses,
            DynamoEnrollmentQueries dynamoEnrollments, EnrollmentRepository enrollments,
            @Value("${migration.batch-size:100}") int batchSize,
            @Value("${migration.dry-run:false}") boolean dryRun,
            MigrationTracker tracker) {
        if (batchSize < 1 || batchSize > 100) {
            throw new IllegalArgumentException("migration.batch-size must be between 1 and 100");
        }
        this.dynamoDepartments = dynamoDepartments;
        this.dynamoInstructors = dynamoInstructors;
        this.departments = departments;
        this.instructors = instructors;
        this.context = context;
        this.dynamoStudents = dynamoStudents;
        this.students = students;
        this.profiles = profiles;
        this.dynamoProfiles = dynamoProfiles;
        this.dynamoCourses = dynamoCourses;
        this.courses = courses;
        this.dynamoEnrollments = dynamoEnrollments;
        this.enrollments = enrollments;
        this.batchSize = batchSize;
        this.dryRun = dryRun;
        this.tracker = tracker;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        try {
            migrate();
            System.exit(SpringApplication.exit(context, () -> 0));
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(SpringApplication.exit(context, () -> 1));
        }
    }

    public void migrate() {
        UUID runId = tracker.start(dryRun, batchSize);
        try {
            AtomicInteger departmentCount = new AtomicInteger();
            AtomicInteger departmentRead = new AtomicInteger();
            forEachPage(dynamoDepartments::findAll, department -> {
                departmentRead.incrementAndGet();
                departments.findById(department.id()).orElseGet(() -> {
                    departmentCount.incrementAndGet();
                    return dryRun ? department : departments.create(department);
                });
            });
            tracker.checkpoint(runId, "DEPARTMENT", departmentRead.get(), departmentCount.get(),
                    departmentRead.get() - departmentCount.get(), null);
            AtomicInteger studentCount = new AtomicInteger();
            AtomicInteger profileCount = new AtomicInteger();
            AtomicInteger studentRead = new AtomicInteger();
            AtomicInteger profileRead = new AtomicInteger();
            forEachPage(dynamoStudents::findAll, student -> {
                studentRead.incrementAndGet();
                students.findById(student.id()).orElseGet(() -> {
                    studentCount.incrementAndGet();
                    return dryRun ? student : students.create(student);
                });
                dynamoProfiles.findByStudentId(student.id()).ifPresent(profile -> {
                    profileRead.incrementAndGet();
                    if (profiles.findByStudentId(profile.studentId()).isEmpty()) {
                        if (!dryRun) profiles.create(profile);
                        profileCount.incrementAndGet();
                    }
                });
            });
            tracker.checkpoint(runId, "STUDENT", studentRead.get(), studentCount.get(),
                    studentRead.get() - studentCount.get(), null);
            tracker.checkpoint(runId, "STUDENT_PROFILE", profileRead.get(), profileCount.get(),
                    profileRead.get() - profileCount.get(), null);
            AtomicInteger instructorCount = new AtomicInteger();
            AtomicInteger instructorRead = new AtomicInteger();
            forEachPage(dynamoInstructors::findAll, instructor -> {
                instructorRead.incrementAndGet();
                instructors.findById(instructor.id()).orElseGet(() -> {
                    instructorCount.incrementAndGet();
                    return dryRun ? instructor : instructors.create(instructor);
                });
            });
            tracker.checkpoint(runId, "INSTRUCTOR", instructorRead.get(), instructorCount.get(),
                    instructorRead.get() - instructorCount.get(), null);
            AtomicInteger courseCount = new AtomicInteger();
            AtomicInteger courseRead = new AtomicInteger();
            forEachPage(dynamoCourses::findAll, course -> {
                courseRead.incrementAndGet();
                if (courses.findById(course.id()).isEmpty()) {
                    if (!dryRun) courses.create(course);
                    courseCount.incrementAndGet();
                }
            });
            tracker.checkpoint(runId, "COURSE", courseRead.get(), courseCount.get(),
                    courseRead.get() - courseCount.get(), null);
            System.out.printf("%s departments=%d instructors=%d students=%d profiles=%d%n",
                    dryRun ? "Would migrate" : "Migrated",
                    departmentCount.get(), instructorCount.get(), studentCount.get(), profileCount.get());
            System.out.printf("%s courses=%d%n", dryRun ? "Would migrate" : "Migrated", courseCount.get());
            AtomicInteger enrollmentCount = new AtomicInteger();
            AtomicInteger enrollmentRead = new AtomicInteger();
            forEachPage(dynamoEnrollments::findAll, enrollment -> {
                enrollmentRead.incrementAndGet();
                if (enrollments.findById(enrollment.id()).isEmpty()) {
                    if (!dryRun) enrollments.create(enrollment);
                    enrollmentCount.incrementAndGet();
                }
            });
            tracker.checkpoint(runId, "ENROLLMENT", enrollmentRead.get(), enrollmentCount.get(),
                    enrollmentRead.get() - enrollmentCount.get(), null);
            System.out.printf("%s enrollments=%d%n", dryRun ? "Would migrate" : "Migrated", enrollmentCount.get());
            tracker.complete(runId, false);
        } catch (Exception exception) {
            tracker.fail(runId, null, null, exception);
            throw new IllegalStateException("DynamoDB to PostgreSQL migration failed", exception);
        }
    }

    private <T> void forEachPage(Function<CursorRequest, com.abc.studentportal.common.pagination.CursorPage<T>> query,
            Consumer<T> consumer) {
        String cursor = null;
        do {
            var page = query.apply(new CursorRequest(batchSize, cursor));
            page.content().forEach(consumer);
            cursor = page.hasNext() ? page.nextCursor() : null;
            if (page.hasNext() && (cursor == null || cursor.isBlank())) {
                throw new IllegalStateException("Migration source returned hasNext without a cursor");
            }
        } while (cursor != null);
    }
}
