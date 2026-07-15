package com.abc.studentportal.common.observability;

import com.abc.studentportal.common.configuration.DynamoDbProperties;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;

import java.util.List;

@Component("dynamoDb")
@Profile({"local-dynamodb", "test-dynamodb", "migration"})
public class DynamoDbHealthIndicator implements HealthIndicator {

    private final DynamoDbClient client;

    private final List<String> tables;

    public DynamoDbHealthIndicator(DynamoDbClient client, DynamoDbProperties properties) {

        this.client = client;
        DynamoDbProperties.Tables names = properties.tables();
        this.tables = List.of(names.departments(), names.students(), names.studentProfiles(), names.instructors(),
                names.courses(), names.enrollments());
    }

    @Override
    public Health health() {

        try {
            List<String> inactive = tables.stream().filter(this::notActive).toList();
            if (!inactive.isEmpty())
                return Health.down().withDetail("tableCount", tables.size())
                        .withDetail("inactiveTables", inactive).build();
            return Health.up().withDetail("tableCount", tables.size()).build();
        } catch (RuntimeException exception) {
            return Health.down().withDetail("tableCount", tables.size())
                    .withDetail("error", exception.getClass().getSimpleName()).build();
        }
    }

    private boolean notActive(String table) {

        return client.describeTable(software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest.builder()
                .tableName(table).build()).table().tableStatus() != TableStatus.ACTIVE;
    }

}
