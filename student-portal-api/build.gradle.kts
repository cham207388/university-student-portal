plugins {
	java
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

group = "com.abc.studentportal"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(platform("software.amazon.awssdk:bom:${libs.versions.aws.sdk.get()}"))
	implementation(libs.bundles.aws.dynamodb)
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.spring.boot.starter.flyway)
	implementation(libs.spring.boot.starter.webmvc)
	implementation(libs.springdoc.openapi.webmvc.ui)
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.postgresql)
	implementation(libs.flyway.core)
	implementation(libs.flyway.database.postgresql)

	compileOnly(libs.lombok)
	annotationProcessor(libs.lombok)

	testImplementation(libs.spring.boot.starter.webmvc.test)
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.localstack)
	testImplementation(libs.testcontainers.postgresql)
	testCompileOnly(libs.lombok)
	testAnnotationProcessor(libs.lombok)
	testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.test {
    systemProperty("spring.autoconfigure.exclude", "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
	useJUnitPlatform {
		excludeTags("dynamodb-integration")
		excludeTags("postgres-integration")
		excludeTags("localstack-rds")
	}
}

val dynamodbIntegrationTest by tasks.registering(Test::class) {
	description = "Runs the DynamoDB persistence tests against a LocalStack container."
	group = LifecycleBasePlugin.VERIFICATION_GROUP
	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = sourceSets.test.get().runtimeClasspath
	shouldRunAfter(tasks.test)
	useJUnitPlatform {
		includeTags("dynamodb-integration")
	}
}

val postgresIntegrationTest by tasks.registering(Test::class) {
	description = "Runs PostgreSQL persistence foundation tests against a Testcontainers database."
	group = LifecycleBasePlugin.VERIFICATION_GROUP
	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = sourceSets.test.get().runtimeClasspath
	shouldRunAfter(tasks.test)
	systemProperty("spring.autoconfigure.exclude", "")
	useJUnitPlatform { includeTags("postgres-integration") }
}

val localstackRdsIntegrationTest by tasks.registering(Test::class) {
    description = "Runs CRUD validation against the provisioned LocalStack RDS PostgreSQL instance."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("localstack-rds") }
    systemProperty("spring.autoconfigure.exclude", "")
}

tasks.check {
	dependsOn(dynamodbIntegrationTest)
	dependsOn(postgresIntegrationTest)
}
