package com.abc.studentportal.common.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.Locale;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
	private static final String PROBLEM_REF = "#/components/schemas/ProblemDetail";

	@Bean
	OpenAPI studentPortalOpenApi() {
		Schema<?> fieldError = new ObjectSchema()
				.addProperty("field", new Schema<String>().type("string").example("email"))
				.addProperty("message", new Schema<String>().type("string").example("must be a well-formed email address"));
		Schema<?> problem = new ObjectSchema()
				.description("RFC 9457 Problem Details response")
				.addProperty("type", new Schema<String>().type("string").format("uri")
						.example("https://student-portal.example/problems/invalid-request"))
				.addProperty("title", new Schema<String>().type("string").example("Bad Request"))
				.addProperty("status", new Schema<Integer>().type("integer").format("int32").example(400))
				.addProperty("detail", new Schema<String>().type("string").example("Unsupported query parameter(s): sort"))
				.addProperty("instance", new Schema<String>().type("string").format("uri"))
				.addProperty("fieldErrors", new ArraySchema().items(fieldError));
		return new OpenAPI()
				.info(new Info().title("University Student Portal API").version("v1")
						.description("DynamoDB-backed university portal. Collection pagination uses a bounded limit and an opaque, query-bound cursor; arbitrary offsets, totals, sorting, and undeclared filters are rejected."))
				.components(new Components().addSchemas("ProblemDetail", problem));
	}

	@Bean
	OperationCustomizer studentPortalOperationCustomizer() {
		return (operation, handler) -> {
			if (operation.getSummary() == null)
				operation.setSummary(summary(handler));
			if (operation.getDescription() == null)
				operation.setDescription(description(handler));
			if (operation.getParameters() != null) {
				operation.getParameters().forEach(parameter -> {
					if ("cursor".equals(parameter.getName())) {
						parameter.setDescription("Opaque DynamoDB continuation token bound to the table, index, partition, and filter values that issued it.");
						parameter.setExample("eyJ2IjoxLCJ0YWJsZSI6Ii4uLiJ9");
					} else if ("limit".equals(parameter.getName())) {
						parameter.setDescription("Maximum items to return. Exact alternate-key filters always return a zero-or-one page with limit 1.");
					}
				});
			}
			addProblem(operation, "400", "Invalid request, validation failure, unsupported query, or malformed cursor");
			addProblem(operation, "404", "Requested resource was not found");
			addProblem(operation, "409", "Domain, uniqueness, dependency, capacity, or optimistic concurrency conflict");
			addProblem(operation, "500", "Unexpected internal error");
			return operation;
		};
	}

	private static void addProblem(io.swagger.v3.oas.models.Operation operation, String status, String description) {
		if (operation.getResponses().containsKey(status))
			return;
		Example example = new Example().value(java.util.Map.of("type",
				"https://student-portal.example/problems/invalid-request", "title", "Bad Request", "status", 400,
				"detail", "Request validation failed"));
		MediaType media = new MediaType().schema(new Schema<>().$ref(PROBLEM_REF)).addExamples("problem", example);
		operation.getResponses().addApiResponse(status,
				new ApiResponse().description(description).content(new Content().addMediaType("application/problem+json", media)));
	}

	private static String summary(HandlerMethod handler) {
		String resource = handler.getBeanType().getSimpleName().replace("Controller", "");
		String action = handler.getMethod().getName().replaceAll("([a-z])([A-Z])", "$1 $2");
		return (action + " " + resource).toLowerCase(Locale.ROOT);
	}

	private static String description(HandlerMethod handler) {
		return "Executes the " + handler.getMethod().getName() + " operation through the DynamoDB application service and documented bounded access pattern.";
	}
}
