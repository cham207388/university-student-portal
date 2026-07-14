package com.abc.studentportal.common.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile({"local-dynamodb", "test-dynamodb", "migration"})
@EnableConfigurationProperties(DynamoDbProperties.class)
public class DynamoDbConfiguration {
}
