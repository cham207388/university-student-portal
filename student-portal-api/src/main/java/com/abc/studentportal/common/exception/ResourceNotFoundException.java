package com.abc.studentportal.common.exception;

import org.springframework.http.HttpStatus;

public final class ResourceNotFoundException extends ApiException {

	public ResourceNotFoundException(String resource, Object id) {
		super(HttpStatus.NOT_FOUND, "resource-not-found", resource + " not found: " + id);
	}
}
