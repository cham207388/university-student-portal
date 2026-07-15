package com.abc.studentportal.department.api;

import com.abc.studentportal.common.api.CursorPageResponse;
import com.abc.studentportal.common.api.CursorResponses;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.api.CourseApi;
import com.abc.studentportal.course.api.CourseMapper;
import com.abc.studentportal.department.application.DepartmentService;
import com.abc.studentportal.instructor.api.InstructorApi;
import com.abc.studentportal.instructor.api.InstructorMapper;
import com.abc.studentportal.student.api.StudentApi;
import com.abc.studentportal.student.api.StudentMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/departments")
@Profile({"local-dynamodb", "test-dynamodb", "local-postgres", "test-postgres"})
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    ResponseEntity<DepartmentApi.Response> create(@Valid @RequestBody DepartmentApi.CreateRequest request) {

        var created = departmentService
                .create(new DepartmentService.CreateCommand(request.code(), request.name(), request.description()));
        return ResponseEntity.created(URI.create("/api/v1/departments/" + created.id()))
                .body(DepartmentMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    DepartmentApi.Response get(@PathVariable UUID id) {

        return DepartmentMapper.toResponse(departmentService.get(id));
    }

    @GetMapping
    CursorPageResponse<DepartmentApi.Response> list(@RequestParam(required = false) String code,
                                                    @RequestParam(defaultValue = "20") int limit,
                                                    @RequestParam(required = false) String cursor) {

        return CursorResponses.page(departmentService.list(new DepartmentService.DepartmentListQuery(code, limit, cursor)),
                DepartmentMapper::toResponse);
    }

    @PutMapping("/{id}")
    DepartmentApi.Response update(@PathVariable UUID id, @Valid @RequestBody DepartmentApi.UpdateRequest request) {

        return DepartmentMapper
                .toResponse(departmentService.update(id, new DepartmentService.UpdateCommand(request.code(), request.name(),
                        request.description(), request.version())));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {

        departmentService.delete(id, version);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/students")
    CursorPageResponse<StudentApi.Response> students(@PathVariable UUID id,
                                                     @RequestParam(required = false) String lastName,
                                                     @RequestParam(defaultValue = "20") int limit,
                                                     @RequestParam(required = false) String cursor) {

        return CursorResponses.page(departmentService.listStudents(id, lastName, new CursorRequest(limit, cursor)),
                StudentMapper::toResponse);
    }

    @GetMapping("/{id}/instructors")
    CursorPageResponse<InstructorApi.Response> instructors(@PathVariable UUID id,
                                                           @RequestParam(defaultValue = "20") int limit,
                                                           @RequestParam(required = false) String cursor) {

        return CursorResponses.page(departmentService.listInstructors(id, new CursorRequest(limit, cursor)),
                InstructorMapper::toResponse);
    }

    @GetMapping("/{id}/courses")
    CursorPageResponse<CourseApi.Response> courses(@PathVariable UUID id,
                                                   @RequestParam(defaultValue = "20") int limit,
                                                   @RequestParam(required = false) String cursor) {

        return CursorResponses.page(departmentService.listCourses(id, new CursorRequest(limit, cursor)),
                CourseMapper::toResponse);
    }

}
