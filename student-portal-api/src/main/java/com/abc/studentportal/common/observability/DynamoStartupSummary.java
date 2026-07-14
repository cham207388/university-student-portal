package com.abc.studentportal.common.observability;

import com.abc.studentportal.common.configuration.DynamoDbProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;

@Component
@Profile({ "local-dynamodb", "test-dynamodb", "migration" })
public class DynamoStartupSummary implements ApplicationRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamoStartupSummary.class);
	private final DynamoDbProperties properties;
	private final Environment environment;

	public DynamoStartupSummary(DynamoDbProperties properties, Environment environment) {
		this.properties = properties;
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) {
		URI endpoint = properties.endpoint();
		DynamoDbProperties.Tables tables = properties.tables();
		LOGGER.info("event=startup_configuration persistence=dynamodb profiles={} region={} endpoint={}://{}:{} tableCount=6 tables={}",
				Arrays.toString(environment.getActiveProfiles()), properties.region(), endpoint.getScheme(),
				endpoint.getHost(), endpoint.getPort(),
				java.util.List.of(tables.departments(), tables.students(), tables.studentProfiles(), tables.instructors(),
						tables.courses(), tables.enrollments()));
	}
}
