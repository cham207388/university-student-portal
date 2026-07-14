package com.abc.studentportal.enrollment.persistence.dynamodb;

import com.abc.studentportal.common.persistence.dynamodb.AbstractDynamoRepository;
import com.abc.studentportal.common.persistence.dynamodb.DynamoDbTables;
import com.abc.studentportal.common.persistence.dynamodb.DynamoPersistenceAdapter;
import com.abc.studentportal.common.persistence.dynamodb.DynamoQueries;
import com.abc.studentportal.enrollment.application.EnrollmentRepository;
import com.abc.studentportal.enrollment.domain.Enrollment;

import java.util.Optional;
import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoEnrollmentRepository extends AbstractDynamoRepository<Enrollment, EnrollmentDynamoRecord>
		implements EnrollmentRepository {
	public DynamoEnrollmentRepository(DynamoDbTables tables) {
		super(tables.enrollments(), "id", EnrollmentDynamoMapper::toRecord, EnrollmentDynamoMapper::toDomain,
				value -> value.id().toString());
	}
	@Override public Enrollment create(Enrollment value) { return createItem(value); }
	@Override public Enrollment update(Enrollment value) { return updateItem(value); }
	@Override public Optional<Enrollment> findById(UUID id) { return findItem(id.toString()); }
	@Override public boolean existsActiveByStudentIdAndCourseId(UUID studentId, UUID courseId) {
		return table().getItem(request -> request.key(key("ACTIVE#" + studentId + "#" + courseId))
				.consistentRead(true)) != null;
	}
	@Override public boolean existsByStudentId(UUID id) { return DynamoQueries.exists(table().index("enrollments-by-student"), id.toString()); }
	@Override public boolean existsByCourseId(UUID id) { return DynamoQueries.exists(table().index("enrollments-by-course"), id.toString()); }
	@Override public void delete(Enrollment value) { deleteItem(value, value.version()); }
}
