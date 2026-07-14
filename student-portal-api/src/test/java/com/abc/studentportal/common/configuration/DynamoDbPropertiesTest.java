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
		assertThat(properties.tableName()).isEqualTo("student-portal");
	}
}
