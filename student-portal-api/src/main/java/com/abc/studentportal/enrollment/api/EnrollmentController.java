package com.abc.studentportal.enrollment.api;

import com.abc.studentportal.common.api.CursorPageResponse;
import com.abc.studentportal.common.api.CursorResponses;
import com.abc.studentportal.enrollment.application.EnrollmentService;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
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
@RequestMapping("/api/v1/enrollments")
@Profile({"local-dynamodb", "test-dynamodb", "local-postgres", "test-postgres"})
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    ResponseEntity<EnrollmentApi.Response> create(@Valid @RequestBody EnrollmentApi.CreateRequest request) {

        var value = enrollmentService.enroll(request.studentId(), request.courseId());
        return ResponseEntity.created(URI.create("/api/v1/enrollments/" + value.id()))
                .body(EnrollmentMapper.toResponse(value));
    }

    @GetMapping("/{id}")
    EnrollmentApi.Response get(@PathVariable UUID id) {

        return EnrollmentMapper.toResponse(enrollmentService.get(id));
    }

    @GetMapping
    CursorPageResponse<EnrollmentApi.Response> list(@RequestParam(required = false) UUID studentId,
                                                    @RequestParam(required = false) UUID courseId,
                                                    @RequestParam(required = false) EnrollmentStatus status,
                                                    @RequestParam(required = false) Instant enrolledFrom,
                                                    @RequestParam(required = false) Instant enrolledTo,
                                                    @RequestParam(defaultValue = "20") int limit,
                                                    @RequestParam(required = false) String cursor) {

        return CursorResponses.page(
                enrollmentService.list(new EnrollmentService.EnrollmentListQuery(studentId, courseId, status, enrolledFrom,
                        enrolledTo, limit, cursor)),
                EnrollmentMapper::toResponse);
    }

    @PatchMapping("/{id}/status")
    EnrollmentApi.Response status(@PathVariable UUID id,
                                  @Valid @RequestBody EnrollmentApi.StatusRequest request) {

        return EnrollmentMapper
                .toResponse(enrollmentService.changeStatus(id, request.status(), request.finalGrade(), request.version()));
    }

    @DeleteMapping("/{id}")
    EnrollmentApi.Response drop(@PathVariable UUID id, @RequestParam long version) {

        return EnrollmentMapper.toResponse(enrollmentService.drop(id, version));
    }

}
