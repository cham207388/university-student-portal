package com.abc.studentportal.enrollment.api;

import com.abc.studentportal.common.api.CursorPageResponse;
import com.abc.studentportal.common.exception.InvalidRequestException;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.enrollment.application.DynamoEnrollmentQueries;
import com.abc.studentportal.enrollment.application.EnrollmentService;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

@RestController
@Profile({"local-dynamodb", "test-dynamodb", "local-postgres", "test-postgres"})
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private final EnrollmentService service;

    private final DynamoEnrollmentQueries queries;

    public EnrollmentController(EnrollmentService service, DynamoEnrollmentQueries queries) {
        this.service = service;
        this.queries = queries;
    }

    @PostMapping
    ResponseEntity<EnrollmentApi.Response> create(@Valid @RequestBody EnrollmentApi.CreateRequest request) {
        var value = service.enroll(request.studentId(), request.courseId());
        return ResponseEntity.created(URI.create("/api/v1/enrollments/" + value.id()))
                .body(EnrollmentMapper.toResponse(value));
    }

    @GetMapping("/{id}")
    EnrollmentApi.Response get(@PathVariable UUID id) {
        return EnrollmentMapper.toResponse(service.get(id));
    }

    @GetMapping
    CursorPageResponse<EnrollmentApi.Response> list(@RequestParam(required = false) UUID studentId,
                                                    @RequestParam(required = false) UUID courseId, @RequestParam(required = false) EnrollmentStatus status,
                                                    @RequestParam(required = false) Instant enrolledFrom, @RequestParam(required = false) Instant enrolledTo,
                                                    @RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String cursor) {
        int filters = (studentId == null ? 0 : 1) + (courseId == null ? 0 : 1) + (status == null ? 0 : 1);
        if (filters > 1)
            throw new InvalidRequestException(
                    "DynamoDB enrollment lists support one relationship or status filter at a time");
        if ((enrolledFrom != null || enrolledTo != null) && studentId == null && courseId == null)
            throw new InvalidRequestException("Enrollment date ranges require studentId or courseId");
        var request = new CursorRequest(limit, cursor);
        CursorPage<com.abc.studentportal.enrollment.domain.Enrollment> page = studentId != null
                ? queries.findByStudent(studentId, enrolledFrom, enrolledTo, request)
                : courseId != null ? queries.findByCourse(courseId, enrolledFrom, enrolledTo, request)
                  : status != null ? queries.findByStatus(status, request) : queries.findAll(request);
        return page(page, EnrollmentMapper::toResponse);
    }

    @PatchMapping("/{id}/status")
    EnrollmentApi.Response status(@PathVariable UUID id,
                                  @Valid @RequestBody EnrollmentApi.StatusRequest request) {
        return EnrollmentMapper
                .toResponse(service.changeStatus(id, request.status(), request.finalGrade(), request.version()));
    }

    @DeleteMapping("/{id}")
    EnrollmentApi.Response drop(@PathVariable UUID id, @RequestParam long version) {
        return EnrollmentMapper.toResponse(service.drop(id, version));
    }

    private static <D, R> CursorPageResponse<R> page(CursorPage<D> page, Function<D, R> mapper) {
        return new CursorPageResponse<>(page.content().stream().map(mapper).toList(), page.limit(), page.nextCursor(),
                page.hasNext());
    }

}
