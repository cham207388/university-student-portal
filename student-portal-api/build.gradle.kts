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
	implementation(libs.spring.boot.starter.webmvc)
	implementation(libs.springdoc.openapi.webmvc.ui)

	compileOnly(libs.lombok)
	annotationProcessor(libs.lombok)

	testImplementation(libs.spring.boot.starter.webmvc.test)
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.localstack)
	testCompileOnly(libs.lombok)
	testAnnotationProcessor(libs.lombok)
	testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.test {
	useJUnitPlatform {
		excludeTags("dynamodb-integration")
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

tasks.check {
	dependsOn(dynamodbIntegrationTest)
}
