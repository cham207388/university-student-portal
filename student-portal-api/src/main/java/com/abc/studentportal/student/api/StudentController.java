package com.abc.studentportal.student.api;

import com.abc.studentportal.common.api.CursorPageResponse;
import com.abc.studentportal.common.application.StudentCourseQueries;
import com.abc.studentportal.common.exception.InvalidRequestException;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.api.CourseApi;
import com.abc.studentportal.course.api.CourseMapper;
import com.abc.studentportal.enrollment.api.EnrollmentApi;
import com.abc.studentportal.enrollment.api.EnrollmentMapper;
import com.abc.studentportal.enrollment.application.EnrollmentQueries;
import com.abc.studentportal.student.application.StudentQueries;
import com.abc.studentportal.student.application.StudentService;
import com.abc.studentportal.student.domain.StudentStatus;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@RestController
@Profile({"local-dynamodb", "test-dynamodb", "local-postgres", "test-postgres"})
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService service;

    private final StudentQueries queries;

    private final EnrollmentQueries enrollments;

    private final StudentCourseQueries relationships;

    public StudentController(StudentService service, StudentQueries queries, EnrollmentQueries enrollments,
                             StudentCourseQueries relationships) {

        this.service = service;
        this.queries = queries;
        this.enrollments = enrollments;
        this.relationships = relationships;
    }

    @PostMapping
    ResponseEntity<StudentApi.Response> create(@Valid @RequestBody StudentApi.CreateRequest request) {

        var value = service.create(
                new StudentService.CreateCommand(request.studentNumber(), request.firstName(), request.lastName(),
                        request.email(), request.status(), request.departmentId()));
        return ResponseEntity.created(URI.create("/api/v1/students/" + value.id()))
                .body(StudentMapper.toResponse(value));
    }

    @GetMapping("/{id}")
    StudentApi.Response get(@PathVariable UUID id) {

        return StudentMapper.toResponse(service.get(id));
    }

    @GetMapping
    CursorPageResponse<StudentApi.Response> list(@RequestParam(required = false) UUID departmentId,
                                                 @RequestParam(required = false) StudentStatus status, @RequestParam(required = false) String lastName,
                                                 @RequestParam(required = false) String studentNumber, @RequestParam(required = false) String email,
                                                 @RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String cursor) {

        int filters = (departmentId == null ? 0 : 1) + (status == null ? 0 : 1)
                + (studentNumber == null ? 0 : 1) + (email == null ? 0 : 1);
        if (filters > 1)
            throw new InvalidRequestException("DynamoDB student lists support one filter at a time");
        if (lastName != null && departmentId == null)
            throw new InvalidRequestException("lastName requires departmentId in DynamoDB mode");
        if (studentNumber != null || email != null) {
            if (cursor != null)
                throw new InvalidRequestException("cursor cannot be combined with an exact student lookup");
            var value = studentNumber != null ? service.findByStudentNumber(studentNumber) : service.findByEmail(email);
            return exact(value.map(StudentMapper::toResponse).stream().toList());
        }
        var request = new CursorRequest(limit, cursor);
        CursorPage<com.abc.studentportal.student.domain.Student> page = departmentId != null
                ? queries.findByDepartment(departmentId, lastName, request)
                : status != null ? queries.findByStatus(status, request) : queries.findAll(request);
        return page(page, StudentMapper::toResponse);
    }

    @PutMapping("/{id}")
    StudentApi.Response update(@PathVariable UUID id, @Valid @RequestBody StudentApi.UpdateRequest request) {

        return StudentMapper.toResponse(
                service.update(id, new StudentService.UpdateCommand(request.studentNumber(), request.firstName(),
                        request.lastName(), request.email(), request.departmentId(), request.version())));
    }

    @PatchMapping("/{id}/status")
    StudentApi.Response status(@PathVariable UUID id,
                               @Valid @RequestBody StudentApi.StatusRequest request) {

        return StudentMapper.toResponse(service.changeStatus(id, request.status(), request.version()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {

        service.delete(id, version);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/profile")
    StudentApi.ProfileResponse putProfile(@PathVariable UUID id,
                                          @Valid @RequestBody StudentApi.ProfileRequest request) {

        return StudentMapper.toResponse(service.putProfile(id, new StudentService.ProfileCommand(request.dateOfBirth(),
                request.phoneNumber(), request.addressLine1(), request.addressLine2(), request.city(), request.state(),
                request.postalCode(), request.country(), request.version())));
    }

    @GetMapping("/{id}/profile")
    StudentApi.ProfileResponse profile(@PathVariable UUID id) {

        return StudentMapper.toResponse(service.getProfile(id));
    }

    @DeleteMapping("/{id}/profile")
    ResponseEntity<Void> deleteProfile(@PathVariable UUID id, @RequestParam long version) {

        service.deleteProfile(id, version);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/enrollments")
    CursorPageResponse<EnrollmentApi.Response> enrollments(@PathVariable UUID id,
                                                           @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
                                                           @RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String cursor) {

        service.get(id);
        return page(enrollments.findByStudent(id, from, to, new CursorRequest(limit, cursor)),
                EnrollmentMapper::toResponse);
    }

    @GetMapping("/{id}/courses")
    CursorPageResponse<CourseApi.Response> courses(@PathVariable UUID id,
                                                   @RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String cursor) {

        service.get(id);
        return page(relationships.findCoursesByStudent(id, new CursorRequest(limit, cursor)), CourseMapper::toResponse);
    }

    private static <D, R> CursorPageResponse<R> page(CursorPage<D> page, Function<D, R> mapper) {

        return new CursorPageResponse<>(page.content().stream().map(mapper).toList(), page.limit(), page.nextCursor(),
                page.hasNext());
    }

    private static <R> CursorPageResponse<R> exact(List<R> content) {

        return new CursorPageResponse<>(content, 1, null, false);
    }

}
