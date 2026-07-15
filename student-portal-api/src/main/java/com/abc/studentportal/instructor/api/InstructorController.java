package com.abc.studentportal.instructor.api;

import com.abc.studentportal.common.api.CursorPageResponse;
import com.abc.studentportal.common.api.CursorResponses;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.api.CourseApi;
import com.abc.studentportal.course.api.CourseMapper;
import com.abc.studentportal.instructor.application.InstructorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/instructors")
@Profile({"local-dynamodb", "test-dynamodb", "local-postgres", "test-postgres"})
public class InstructorController {

    private final InstructorService instructorService;

    @PostMapping
    ResponseEntity<InstructorApi.Response> create(@Valid @RequestBody InstructorApi.CreateRequest request) {

        var value = instructorService.create(new InstructorService.CreateCommand(request.employeeNumber(), request.firstName(),
                request.lastName(), request.email(), request.departmentId()));
        return ResponseEntity.created(URI.create("/api/v1/instructors/" + value.id()))
                .body(InstructorMapper.toResponse(value));
    }

    @GetMapping("/{id}")
    InstructorApi.Response get(@PathVariable UUID id) {

        return InstructorMapper.toResponse(instructorService.get(id));
    }

    @GetMapping
    CursorPageResponse<InstructorApi.Response> list(@RequestParam(required = false) UUID departmentId,
                                                    @RequestParam(required = false) String employeeNumber,
                                                    @RequestParam(required = false) String email,
                                                    @RequestParam(defaultValue = "20") int limit,
                                                    @RequestParam(required = false) String cursor) {

        return CursorResponses.page(
                instructorService.list(new InstructorService.InstructorListQuery(departmentId, employeeNumber, email, limit,
                        cursor)),
                InstructorMapper::toResponse);
    }

    @PutMapping("/{id}")
    InstructorApi.Response update(@PathVariable UUID id, @Valid @RequestBody InstructorApi.UpdateRequest request) {

        return InstructorMapper.toResponse(instructorService.update(id,
                new InstructorService.UpdateCommand(request.employeeNumber(),
                        request.firstName(), request.lastName(), request.email(), request.departmentId(),
                        request.version())));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {

        instructorService.delete(id, version);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/courses")
    CursorPageResponse<CourseApi.Response> courses(@PathVariable UUID id,
                                                   @RequestParam(defaultValue = "20") int limit,
                                                   @RequestParam(required = false) String cursor) {

        return CursorResponses.page(instructorService.listCourses(id, new CursorRequest(limit, cursor)),
                CourseMapper::toResponse);
    }

}
