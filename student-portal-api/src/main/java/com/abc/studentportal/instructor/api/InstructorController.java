package com.abc.studentportal.instructor.api;

import com.abc.studentportal.common.api.CursorPageResponse;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.api.CourseApi;
import com.abc.studentportal.course.api.CourseMapper;
import com.abc.studentportal.course.application.CourseQueries;
import com.abc.studentportal.instructor.application.InstructorQueries;
import com.abc.studentportal.instructor.application.InstructorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/instructors")
@Profile({"local-dynamodb", "test-dynamodb", "local-postgres", "test-postgres"})
public class InstructorController {

    private final InstructorService service;

    private final InstructorQueries queries;

    private final CourseQueries courses;

    @PostMapping
    ResponseEntity<InstructorApi.Response> create(@Valid @RequestBody InstructorApi.CreateRequest request) {

        var value = service.create(new InstructorService.CreateCommand(request.employeeNumber(), request.firstName(),
                request.lastName(), request.email(), request.departmentId()));
        return ResponseEntity.created(URI.create("/api/v1/instructors/" + value.id()))
                .body(InstructorMapper.toResponse(value));
    }

    @GetMapping("/{id}")
    InstructorApi.Response get(@PathVariable UUID id) {

        return InstructorMapper.toResponse(service.get(id));
    }

    @GetMapping
    CursorPageResponse<InstructorApi.Response> list(@RequestParam(required = false) UUID departmentId,
                                                    @RequestParam(required = false) String employeeNumber, @RequestParam(required = false) String email,
                                                    @RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String cursor) {

        int filters = (departmentId == null ? 0 : 1) + (employeeNumber == null ? 0 : 1) + (email == null ? 0 : 1);
        if (filters > 1)
            throw new com.abc.studentportal.common.exception.InvalidRequestException(
                    "DynamoDB instructor lists support one filter at a time");
        if (employeeNumber != null || email != null) {
            if (cursor != null)
                throw new com.abc.studentportal.common.exception.InvalidRequestException(
                        "cursor cannot be combined with an exact instructor lookup");
            var value = employeeNumber != null ? service.findByEmployeeNumber(employeeNumber) : service.findByEmail(email);
            return exact(value.map(InstructorMapper::toResponse).stream().toList());
        }
        var request = new CursorRequest(limit, cursor);
        return page(departmentId == null ? queries.findAll(request) : queries.findByDepartment(departmentId, request),
                InstructorMapper::toResponse);
    }

    @PutMapping("/{id}")
    InstructorApi.Response update(@PathVariable UUID id, @Valid @RequestBody InstructorApi.UpdateRequest request) {

        return InstructorMapper.toResponse(service.update(id,
                new InstructorService.UpdateCommand(request.employeeNumber(),
                        request.firstName(), request.lastName(), request.email(), request.departmentId(),
                        request.version())));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {

        service.delete(id, version);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/courses")
    CursorPageResponse<CourseApi.Response> courses(@PathVariable UUID id,
                                                   @RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String cursor) {

        service.get(id);
        return page(courses.findByInstructor(id, new CursorRequest(limit, cursor)), CourseMapper::toResponse);
    }

    private static <D, R> CursorPageResponse<R> page(CursorPage<D> page, Function<D, R> mapper) {

        return new CursorPageResponse<>(page.content().stream().map(mapper).toList(), page.limit(), page.nextCursor(),
                page.hasNext());
    }

    private static <R> CursorPageResponse<R> exact(List<R> content) {

        return new CursorPageResponse<>(content, 1, null, false);
    }

}
