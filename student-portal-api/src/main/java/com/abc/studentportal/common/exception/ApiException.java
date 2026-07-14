package com.abc.studentportal.common.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

public abstract class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final URI type;

	protected ApiException(HttpStatus status, String problemType, String message) {
		super(message);
		this.status = status;
		this.type = URI.create("https://student-portal.example/problems/" + problemType);
	}

	public HttpStatus status() {
		return status;
	}

	public URI type() {
		return type;
	}
}
