package com.abc.studentportal.common.exception;

import com.abc.studentportal.common.api.FieldErrorDetail;
import com.abc.studentportal.common.domain.DomainRuleViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handleApiException(ApiException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
		problem.setTitle(exception.status().getReasonPhrase());
		problem.setType(exception.type());
		return ResponseEntity.status(exception.status()).body(problem);
	}

	@ExceptionHandler(DomainRuleViolationException.class)
	ResponseEntity<ProblemDetail> handleDomainRule(DomainRuleViolationException exception) {
		ProblemDetail problem = problem(HttpStatus.CONFLICT, "domain-rule-violation", exception.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation-failed", "Request validation failed");
		List<FieldErrorDetail> errors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
				.toList();
		problem.setProperty("fieldErrors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ProblemDetail> handleMalformedRequest() {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "malformed-request", "Request body is malformed or contains an unsupported value");
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException exception) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "invalid-request", exception.getMessage());
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
		LOGGER.error("Unexpected request failure", exception);
		ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
				"An unexpected internal error occurred");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
	}

	private static ProblemDetail problem(HttpStatus status, String type, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(status.getReasonPhrase());
		problem.setType(URI.create("https://student-portal.example/problems/" + type));
		return problem;
	}
}
