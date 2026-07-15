package com.abc.studentportal.student.api;

import com.abc.studentportal.common.api.CursorPageResponse;
import com.abc.studentportal.common.api.CursorResponses;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.api.CourseApi;
import com.abc.studentportal.course.api.CourseMapper;
import com.abc.studentportal.enrollment.api.EnrollmentApi;
import com.abc.studentportal.enrollment.api.EnrollmentMapper;
import com.abc.studentportal.student.application.StudentService;
import com.abc.studentportal.student.domain.StudentStatus;
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
@RequestMapping("/api/v1/students")
@Profile({"local-dynamodb", "test-dynamodb", "local-postgres", "test-postgres"})
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    ResponseEntity<StudentApi.Response> create(@Valid @RequestBody StudentApi.CreateRequest request) {

        var value = studentService.create(
                new StudentService.CreateCommand(request.studentNumber(), request.firstName(), request.lastName(),
                        request.email(), request.status(), request.departmentId()));
        return ResponseEntity.created(URI.create("/api/v1/students/" + value.id()))
                .body(StudentMapper.toResponse(value));
    }

    @GetMapping("/{id}")
    StudentApi.Response get(@PathVariable UUID id) {

        return StudentMapper.toResponse(studentService.get(id));
    }

    @GetMapping
    CursorPageResponse<StudentApi.Response> list(@RequestParam(required = false) UUID departmentId,
                                                 @RequestParam(required = false) StudentStatus status,
                                                 @RequestParam(required = false) String lastName,
                                                 @RequestParam(required = false) String studentNumber,
                                                 @RequestParam(required = false) String email,
                                                 @RequestParam(defaultValue = "20") int limit,
                                                 @RequestParam(required = false) String cursor) {

        return CursorResponses.page(
                studentService.list(new StudentService.StudentListQuery(departmentId, status, lastName, studentNumber, email,
                        limit, cursor)),
                StudentMapper::toResponse);
    }

    @PutMapping("/{id}")
    StudentApi.Response update(@PathVariable UUID id, @Valid @RequestBody StudentApi.UpdateRequest request) {

        return StudentMapper.toResponse(
                studentService.update(id, new StudentService.UpdateCommand(request.studentNumber(), request.firstName(),
                        request.lastName(), request.email(), request.departmentId(), request.version())));
    }

    @PatchMapping("/{id}/status")
    StudentApi.Response status(@PathVariable UUID id,
                               @Valid @RequestBody StudentApi.StatusRequest request) {

        return StudentMapper.toResponse(studentService.changeStatus(id, request.status(), request.version()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {

        studentService.delete(id, version);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/profile")
    StudentApi.ProfileResponse putProfile(@PathVariable UUID id,
                                          @Valid @RequestBody StudentApi.ProfileRequest request) {

        return StudentMapper.toResponse(studentService.putProfile(id, new StudentService.ProfileCommand(request.dateOfBirth(),
                request.phoneNumber(), request.addressLine1(), request.addressLine2(), request.city(), request.state(),
                request.postalCode(), request.country(), request.version())));
    }

    @GetMapping("/{id}/profile")
    StudentApi.ProfileResponse profile(@PathVariable UUID id) {

        return StudentMapper.toResponse(studentService.getProfile(id));
    }

    @DeleteMapping("/{id}/profile")
    ResponseEntity<Void> deleteProfile(@PathVariable UUID id, @RequestParam long version) {

        studentService.deleteProfile(id, version);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/enrollments")
    CursorPageResponse<EnrollmentApi.Response> enrollments(@PathVariable UUID id,
                                                           @RequestParam(required = false) Instant from,
                                                           @RequestParam(required = false) Instant to,
                                                           @RequestParam(defaultValue = "20") int limit,
                                                           @RequestParam(required = false) String cursor) {

        return CursorResponses.page(studentService.listEnrollments(id, from, to, new CursorRequest(limit, cursor)),
                EnrollmentMapper::toResponse);
    }

    @GetMapping("/{id}/courses")
    CursorPageResponse<CourseApi.Response> courses(@PathVariable UUID id,
                                                   @RequestParam(defaultValue = "20") int limit,
                                                   @RequestParam(required = false) String cursor) {

        return CursorResponses.page(studentService.listCourses(id, new CursorRequest(limit, cursor)),
                CourseMapper::toResponse);
    }

}
