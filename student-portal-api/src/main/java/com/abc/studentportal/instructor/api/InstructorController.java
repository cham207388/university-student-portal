package com.abc.studentportal.instructor.api;

import com.abc.studentportal.common.api.CursorPageResponse;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.api.CourseApi;
import com.abc.studentportal.course.api.CourseMapper;
import com.abc.studentportal.course.application.DynamoCourseQueries;
import com.abc.studentportal.instructor.application.DynamoInstructorQueries;
import com.abc.studentportal.instructor.application.InstructorService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;
import java.util.function.Function;

@RestController
@Profile({ "local-dynamodb", "test-dynamodb" })
@RequestMapping("/api/v1/instructors")
public class InstructorController {
	private final InstructorService service;
	private final DynamoInstructorQueries queries;
	private final DynamoCourseQueries courses;

	public InstructorController(InstructorService service, DynamoInstructorQueries queries,
			DynamoCourseQueries courses) {
		this.service = service;
		this.queries = queries;
		this.courses = courses;
	}

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
			@RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String cursor) {
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
}
