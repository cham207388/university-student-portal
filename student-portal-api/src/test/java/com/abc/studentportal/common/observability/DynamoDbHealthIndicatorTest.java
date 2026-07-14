package com.abc.studentportal.common.observability;

import com.abc.studentportal.common.configuration.DynamoDbProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamoDbHealthIndicatorTest {
	@Test
	void reportsAllSixActiveTablesAndSafeFailureDetails() {
		DynamoDbClient client = mock(DynamoDbClient.class);
		when(client.describeTable(any(DescribeTableRequest.class))).thenReturn(DescribeTableResponse.builder()
				.table(TableDescription.builder().tableStatus(TableStatus.ACTIVE).build()).build());
		DynamoDbHealthIndicator indicator = new DynamoDbHealthIndicator(client, properties());
		assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
		assertThat(indicator.health().getDetails()).containsEntry("tableCount", 6);

		when(client.describeTable(any(DescribeTableRequest.class))).thenThrow(new IllegalStateException("secret detail"));
		var down = indicator.health();
		assertThat(down.getStatus()).isEqualTo(Status.DOWN);
		assertThat(down.getDetails()).containsEntry("error", "IllegalStateException");
		assertThat(down.getDetails().toString()).doesNotContain("secret detail");
	}

	private static DynamoDbProperties properties() {
		return new DynamoDbProperties("us-east-1", URI.create("http://localhost:4566"),
				new DynamoDbProperties.Tables("departments", "students", "profiles", "instructors", "courses",
						"enrollments"));
	}
}
