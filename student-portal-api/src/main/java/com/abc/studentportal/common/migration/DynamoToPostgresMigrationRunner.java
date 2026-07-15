package com.abc.studentportal.common.migration;

import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.department.application.DynamoDepartmentQueries;
import com.abc.studentportal.instructor.application.DynamoInstructorQueries;
import com.abc.studentportal.instructor.application.InstructorRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Profile("migration")
public class DynamoToPostgresMigrationRunner implements ApplicationRunner {
    private static final CursorRequest PAGE = new CursorRequest(100, null);
    private final DynamoDepartmentQueries dynamoDepartments;
    private final DynamoInstructorQueries dynamoInstructors;
    private final DepartmentRepository departments;
    private final InstructorRepository instructors;
    private final ConfigurableApplicationContext context;

    public DynamoToPostgresMigrationRunner(DynamoDepartmentQueries dynamoDepartments,
            DynamoInstructorQueries dynamoInstructors, DepartmentRepository departments,
            InstructorRepository instructors, ConfigurableApplicationContext context) {
        this.dynamoDepartments = dynamoDepartments;
        this.dynamoInstructors = dynamoInstructors;
        this.departments = departments;
        this.instructors = instructors;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        try {
            AtomicInteger departmentCount = new AtomicInteger();
            dynamoDepartments.findAll(PAGE).content().forEach(department -> {
                departments.findById(department.id()).orElseGet(() -> {
                    departmentCount.incrementAndGet();
                    return departments.create(department);
                });
            });
            AtomicInteger instructorCount = new AtomicInteger();
            dynamoInstructors.findAll(PAGE).content().forEach(instructor -> {
                instructors.findById(instructor.id()).orElseGet(() -> {
                    instructorCount.incrementAndGet();
                    return instructors.create(instructor);
                });
            });
            System.out.printf("Migrated departments=%d instructors=%d%n", departmentCount.get(), instructorCount.get());
            System.exit(SpringApplication.exit(context, () -> 0));
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(SpringApplication.exit(context, () -> 1));
        }
    }
}
