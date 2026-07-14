package com.abc.studentportal.common.persistence.dynamodb;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repository
@Profile({ "local-dynamodb", "test-dynamodb", "migration" })
public @interface DynamoPersistenceAdapter {
}
