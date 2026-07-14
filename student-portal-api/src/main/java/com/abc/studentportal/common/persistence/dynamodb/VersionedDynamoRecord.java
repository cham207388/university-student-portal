package com.abc.studentportal.common.persistence.dynamodb;

public interface VersionedDynamoRecord {
	Long getVersion();
	void setVersion(Long version);
}
