package com.abc.studentportal.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalExceptionHandlerTestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerMvcTest {

	private final MockMvc mockMvc;

	@Autowired
	GlobalExceptionHandlerMvcTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void returnsFieldLevelValidationProblem() throws Exception {
		mockMvc.perform(post("/test/departments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"code":"","name":"","description":null}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://student-portal.example/problems/validation-failed"))
				.andExpect(jsonPath("$.fieldErrors.length()").value(2));
	}

	@Test
	void returnsSafeNotFoundProblem() throws Exception {
		mockMvc.perform(get("/test/missing"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.type").value("https://student-portal.example/problems/resource-not-found"))
				.andExpect(jsonPath("$.detail").value("Student not found: 42"));
	}

}
