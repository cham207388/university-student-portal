package com.abc.studentportal.common.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local-dynamodb")
class DynamoDbPropertiesTest {

	@Autowired
	private DynamoDbProperties properties;

	@Test
	void bindsLocalDynamoDbDefaults() {
		assertThat(properties.region()).isEqualTo("us-east-1");
		assertThat(properties.endpoint()).isEqualTo(URI.create("http://localhost:4566"));
		assertThat(properties.tables().departments()).isEqualTo("student-portal-departments");
		assertThat(properties.tables().students()).isEqualTo("student-portal-students");
		assertThat(properties.tables().studentProfiles()).isEqualTo("student-portal-student-profiles");
		assertThat(properties.tables().instructors()).isEqualTo("student-portal-instructors");
		assertThat(properties.tables().courses()).isEqualTo("student-portal-courses");
		assertThat(properties.tables().enrollments()).isEqualTo("student-portal-enrollments");
	}
}
