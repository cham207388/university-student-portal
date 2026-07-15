package com.abc.studentportal.department.api;

import com.abc.studentportal.common.api.CursorPageResponse;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.api.CourseApi;
import com.abc.studentportal.course.api.CourseMapper;
import com.abc.studentportal.course.application.DynamoCourseQueries;
import com.abc.studentportal.department.application.DepartmentService;
import com.abc.studentportal.department.application.DynamoDepartmentQueries;
import com.abc.studentportal.instructor.api.InstructorApi;
import com.abc.studentportal.instructor.api.InstructorMapper;
import com.abc.studentportal.instructor.application.DynamoInstructorQueries;
import com.abc.studentportal.student.api.StudentApi;
import com.abc.studentportal.student.api.StudentMapper;
import com.abc.studentportal.student.application.DynamoStudentQueries;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;
import java.util.List;
import java.util.function.Function;

@RestController
@Profile({"local-dynamodb", "test-dynamodb"})
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService service;

    private final DynamoDepartmentQueries departments;

    private final DynamoStudentQueries students;

    private final DynamoInstructorQueries instructors;

    private final DynamoCourseQueries courses;

    public DepartmentController(DepartmentService service, DynamoDepartmentQueries departments,
                                DynamoStudentQueries students, DynamoInstructorQueries instructors, DynamoCourseQueries courses) {
        this.service = service;
        this.departments = departments;
        this.students = students;
        this.instructors = instructors;
        this.courses = courses;
    }

    @PostMapping
    ResponseEntity<DepartmentApi.Response> create(@Valid @RequestBody DepartmentApi.CreateRequest request) {
        var created = service
                .create(new DepartmentService.CreateCommand(request.code(), request.name(), request.description()));
        return ResponseEntity.created(URI.create("/api/v1/departments/" + created.id()))
                .body(DepartmentMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    DepartmentApi.Response get(@PathVariable UUID id) {
        return DepartmentMapper.toResponse(service.get(id));
    }

    @GetMapping
    CursorPageResponse<DepartmentApi.Response> list(@RequestParam(required = false) String code,
                                                    @RequestParam(defaultValue = "20") int limit,
                                                    @RequestParam(required = false) String cursor) {
        if (code != null) {
            requireNoCursor(cursor, "code");
            return exact(service.findByCode(code).map(DepartmentMapper::toResponse).stream().toList());
        }
        return page(departments.findAll(new CursorRequest(limit, cursor)), DepartmentMapper::toResponse);
    }

    @PutMapping("/{id}")
    DepartmentApi.Response update(@PathVariable UUID id, @Valid @RequestBody DepartmentApi.UpdateRequest request) {
        return DepartmentMapper
                .toResponse(service.update(id, new DepartmentService.UpdateCommand(request.code(), request.name(),
                        request.description(), request.version())));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {
        service.delete(id, version);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/students")
    CursorPageResponse<StudentApi.Response> students(@PathVariable UUID id,
                                                     @RequestParam(required = false) String lastName,
                                                     @RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String cursor) {
        service.get(id);
        return page(students.findByDepartment(id, lastName, new CursorRequest(limit, cursor)),
                StudentMapper::toResponse);
    }

    @GetMapping("/{id}/instructors")
    CursorPageResponse<InstructorApi.Response> instructors(@PathVariable UUID id,
                                                           @RequestParam(defaultValue = "20") int limit,
                                                           @RequestParam(required = false) String cursor) {
        service.get(id);
        return page(instructors.findByDepartment(id, new CursorRequest(limit, cursor)), InstructorMapper::toResponse);
    }

    @GetMapping("/{id}/courses")
    CursorPageResponse<CourseApi.Response> courses(@PathVariable UUID id, @RequestParam(defaultValue = "20") int limit,
                                                   @RequestParam(required = false) String cursor) {
        service.get(id);
        return page(courses.findByDepartment(id, new CursorRequest(limit, cursor)), CourseMapper::toResponse);
    }

    private static <D, R> CursorPageResponse<R> page(CursorPage<D> page, Function<D, R> mapper) {
        return new CursorPageResponse<>(page.content().stream().map(mapper).toList(), page.limit(), page.nextCursor(),
                page.hasNext());
    }

    private static <R> CursorPageResponse<R> exact(List<R> content) {
        return new CursorPageResponse<>(content, 1, null, false);
    }

    private static void requireNoCursor(String cursor, String filter) {
        if (cursor != null)
            throw new com.abc.studentportal.common.exception.InvalidRequestException(
                    "cursor cannot be combined with exact " + filter + " lookup");
    }

}
