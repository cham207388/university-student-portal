package com.abc.studentportal.common.exception;

import com.abc.studentportal.department.api.DepartmentApi;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
class GlobalExceptionHandlerTestController {

	@PostMapping("/departments")
	DepartmentApi.CreateRequest create(@Valid @RequestBody DepartmentApi.CreateRequest request) {
		return request;
	}

	@GetMapping("/missing")
	void missing() {
		throw new ResourceNotFoundException("Student", 42);
	}
}
