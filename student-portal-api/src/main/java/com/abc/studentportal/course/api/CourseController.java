package com.abc.studentportal.course.api;

import com.abc.studentportal.common.api.CursorPageResponse;
import com.abc.studentportal.common.application.StudentCourseQueries;
import com.abc.studentportal.common.exception.InvalidRequestException;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.application.CourseQueries;
import com.abc.studentportal.course.application.CourseService;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;
import com.abc.studentportal.enrollment.api.EnrollmentApi;
import com.abc.studentportal.enrollment.api.EnrollmentMapper;
import com.abc.studentportal.enrollment.application.EnrollmentQueries;
import com.abc.studentportal.student.api.StudentApi;
import com.abc.studentportal.student.api.StudentMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
@Profile({"local-dynamodb", "test-dynamodb", "local-postgres", "test-postgres"})
public class CourseController {

    private final CourseService service;

    private final CourseQueries queries;

    private final EnrollmentQueries enrollments;

    private final StudentCourseQueries relationships;

    @PostMapping
    ResponseEntity<CourseApi.Response> create(@Valid @RequestBody CourseApi.CreateRequest request) {

        var value = service
                .create(new CourseService.CreateCommand(request.courseCode(), request.title(), request.description(),
                        request.credits(), request.capacity(), request.status(), request.departmentId(),
                        request.instructorId()));
        return ResponseEntity.created(URI.create("/api/v1/courses/" + value.id())).body(CourseMapper.toResponse(value));
    }

    @GetMapping("/{id}")
    CourseApi.Response get(@PathVariable UUID id) {

        return CourseMapper.toResponse(service.get(id));
    }

    @GetMapping
    CursorPageResponse<CourseApi.Response> list(@RequestParam(required = false) UUID departmentId,
                                                @RequestParam(required = false) UUID instructorId, @RequestParam(required = false) CourseStatus status,
                                                @RequestParam(required = false) String courseCode,
                                                @RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String cursor) {

        int filters = (departmentId == null ? 0 : 1) + (instructorId == null ? 0 : 1) + (status == null ? 0 : 1)
                + (courseCode == null ? 0 : 1);
        if (filters > 1)
            throw new InvalidRequestException("DynamoDB course lists support one filter at a time");
        if (courseCode != null) {
            if (cursor != null)
                throw new InvalidRequestException("cursor cannot be combined with exact courseCode lookup");
            return exact(service.findByCourseCode(courseCode).map(CourseMapper::toResponse).stream().toList());
        }
        var request = new CursorRequest(limit, cursor);
        CursorPage<Course> page = departmentId != null
                ? queries.findByDepartment(departmentId, request)
                : instructorId != null ? queries.findByInstructor(instructorId, request)
                  : status != null ? queries.findByStatus(status, request) : queries.findAll(request);
        return page(page, CourseMapper::toResponse);
    }

    @PutMapping("/{id}")
    CourseApi.Response update(@PathVariable UUID id, @Valid @RequestBody CourseApi.UpdateRequest request) {

        return CourseMapper.toResponse(service.update(id,
                new CourseService.UpdateCommand(request.courseCode(), request.title(),
                        request.description(), request.credits(), request.capacity(), request.departmentId(),
                        request.instructorId(), request.version())));
    }

    @PatchMapping("/{id}/status")
    CourseApi.Response status(@PathVariable UUID id, @Valid @RequestBody CourseApi.StatusRequest request) {

        return CourseMapper.toResponse(service.changeStatus(id, request.status(), request.version()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {

        service.delete(id, version);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/enrollments")
    CursorPageResponse<EnrollmentApi.Response> enrollments(@PathVariable UUID id,
                                                           @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
                                                           @RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String cursor) {

        service.get(id);
        return page(enrollments.findByCourse(id, from, to, new CursorRequest(limit, cursor)),
                EnrollmentMapper::toResponse);
    }

    @GetMapping("/{id}/students")
    CursorPageResponse<StudentApi.Response> students(@PathVariable UUID id,
                                                     @RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String cursor) {

        service.get(id);
        return page(relationships.findStudentsByCourse(id, new CursorRequest(limit, cursor)),
                StudentMapper::toResponse);
    }

    private static <D, R> CursorPageResponse<R> page(CursorPage<D> page, Function<D, R> mapper) {

        return new CursorPageResponse<>(page.content().stream().map(mapper).toList(), page.limit(), page.nextCursor(),
                page.hasNext());
    }

    private static <R> CursorPageResponse<R> exact(List<R> content) {

        return new CursorPageResponse<>(content, 1, null, false);
    }

}
