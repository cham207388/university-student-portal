package com.abc.studentportal.common.configuration;

import com.abc.studentportal.course.persistence.dynamodb.CourseDynamoRecord;
import com.abc.studentportal.department.persistence.dynamodb.DepartmentDynamoRecord;
import com.abc.studentportal.enrollment.persistence.dynamodb.EnrollmentDynamoRecord;
import com.abc.studentportal.instructor.persistence.dynamodb.InstructorDynamoRecord;
import com.abc.studentportal.student.persistence.dynamodb.StudentDynamoRecord;
import com.abc.studentportal.student.persistence.dynamodb.StudentProfileDynamoRecord;
import com.abc.studentportal.common.persistence.dynamodb.DynamoDbTables;
import com.abc.studentportal.common.persistence.dynamodb.DynamoCursorCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration(proxyBeanMethods = false)
@Profile({"local-dynamodb", "test-dynamodb", "migration"})
@EnableConfigurationProperties(DynamoDbProperties.class)
public class DynamoDbConfiguration {

	@Bean(destroyMethod = "close")
	DynamoDbClient dynamoDbClient(DynamoDbProperties properties) {
		return DynamoDbClient.builder()
				.region(Region.of(properties.region()))
				.endpointOverride(properties.endpoint())
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
				.httpClientBuilder(UrlConnectionHttpClient.builder())
				.build();
	}

	@Bean
	DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
		return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
	}

	@Bean
	DynamoCursorCodec dynamoCursorCodec() {
		return new DynamoCursorCodec();
	}

	@Bean
	DynamoDbTables dynamoDbTables(DynamoDbEnhancedClient client, DynamoDbProperties properties) {
		DynamoDbProperties.Tables names = properties.tables();
		return new DynamoDbTables(
				client.table(names.departments(), TableSchema.fromBean(DepartmentDynamoRecord.class)),
				client.table(names.students(), TableSchema.fromBean(StudentDynamoRecord.class)),
				client.table(names.studentProfiles(), TableSchema.fromBean(StudentProfileDynamoRecord.class)),
				client.table(names.instructors(), TableSchema.fromBean(InstructorDynamoRecord.class)),
				client.table(names.courses(), TableSchema.fromBean(CourseDynamoRecord.class)),
				client.table(names.enrollments(), TableSchema.fromBean(EnrollmentDynamoRecord.class)));
	}
}
