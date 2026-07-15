package com.abc.studentportal.course.api;

import com.abc.studentportal.common.api.CursorPageResponse;
import com.abc.studentportal.common.api.CursorResponses;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.application.CourseService;
import com.abc.studentportal.course.domain.CourseStatus;
import com.abc.studentportal.enrollment.api.EnrollmentApi;
import com.abc.studentportal.enrollment.api.EnrollmentMapper;
import com.abc.studentportal.student.api.StudentApi;
import com.abc.studentportal.student.api.StudentMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
@Profile({"local-dynamodb", "test-dynamodb", "local-postgres", "test-postgres"})
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    ResponseEntity<CourseApi.Response> create(@Valid @RequestBody CourseApi.CreateRequest request) {

        var value = courseService
                .create(new CourseService.CreateCommand(request.courseCode(), request.title(), request.description(),
                        request.credits(), request.capacity(), request.status(), request.departmentId(),
                        request.instructorId()));
        return ResponseEntity.created(URI.create("/api/v1/courses/" + value.id())).body(CourseMapper.toResponse(value));
    }

    @GetMapping("/{id}")
    CourseApi.Response get(@PathVariable UUID id) {

        return CourseMapper.toResponse(courseService.get(id));
    }

    @GetMapping
    CursorPageResponse<CourseApi.Response> list(@RequestParam(required = false) UUID departmentId,
                                                @RequestParam(required = false) UUID instructorId,
                                                @RequestParam(required = false) CourseStatus status,
                                                @RequestParam(required = false) String courseCode,
                                                @RequestParam(defaultValue = "20") int limit,
                                                @RequestParam(required = false) String cursor) {

        return CursorResponses.page(
                courseService.list(new CourseService.CourseListQuery(departmentId, instructorId, status, courseCode, limit,
                        cursor)),
                CourseMapper::toResponse);
    }

    @PutMapping("/{id}")
    CourseApi.Response update(@PathVariable UUID id, @Valid @RequestBody CourseApi.UpdateRequest request) {

        return CourseMapper.toResponse(courseService.update(id,
                new CourseService.UpdateCommand(request.courseCode(), request.title(),
                        request.description(), request.credits(), request.capacity(), request.departmentId(),
                        request.instructorId(), request.version())));
    }

    @PatchMapping("/{id}/status")
    CourseApi.Response status(@PathVariable UUID id, @Valid @RequestBody CourseApi.StatusRequest request) {

        return CourseMapper.toResponse(courseService.changeStatus(id, request.status(), request.version()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {

        courseService.delete(id, version);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/enrollments")
    CursorPageResponse<EnrollmentApi.Response> enrollments(@PathVariable UUID id,
                                                           @RequestParam(required = false) Instant from,
                                                           @RequestParam(required = false) Instant to,
                                                           @RequestParam(defaultValue = "20") int limit,
                                                           @RequestParam(required = false) String cursor) {

        return CursorResponses.page(courseService.listEnrollments(id, from, to, new CursorRequest(limit, cursor)),
                EnrollmentMapper::toResponse);
    }

    @GetMapping("/{id}/students")
    CursorPageResponse<StudentApi.Response> students(@PathVariable UUID id,
                                                     @RequestParam(defaultValue = "20") int limit,
                                                     @RequestParam(required = false) String cursor) {

        return CursorResponses.page(courseService.listStudents(id, new CursorRequest(limit, cursor)),
                StudentMapper::toResponse);
    }

}
