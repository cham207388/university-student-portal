package com.abc.studentportal.enrollment.persistence.dynamodb;

import com.abc.studentportal.common.exception.InvalidRequestException;
import com.abc.studentportal.common.persistence.dynamodb.AbstractDynamoRepository;
import com.abc.studentportal.common.persistence.dynamodb.DynamoDbTables;
import com.abc.studentportal.common.persistence.dynamodb.DynamoPersistenceAdapter;
import com.abc.studentportal.common.persistence.dynamodb.DynamoQueries;
import com.abc.studentportal.common.persistence.dynamodb.DynamoSortKeys;
import com.abc.studentportal.common.persistence.dynamodb.DynamoCursorCodec;
import com.abc.studentportal.common.persistence.dynamodb.DynamoCursorQueries;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.enrollment.application.DynamoEnrollmentQueries;
import com.abc.studentportal.enrollment.application.EnrollmentRepository;
import com.abc.studentportal.enrollment.domain.Enrollment;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoEnrollmentRepository extends AbstractDynamoRepository<Enrollment, EnrollmentDynamoRecord>
		implements EnrollmentRepository, DynamoEnrollmentQueries {
	private final DynamoCursorCodec cursorCodec;
	public DynamoEnrollmentRepository(DynamoDbTables tables, DynamoCursorCodec cursorCodec) {
		super(tables.enrollments(), "id", EnrollmentDynamoMapper::toRecord, EnrollmentDynamoMapper::toDomain,
				value -> value.id().toString());
		this.cursorCodec = cursorCodec;
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
	@Override public CursorPage<Enrollment> findAll(CursorRequest request) {
		return query("enrollments-catalog", "ENROLLMENT", null, null, request);
	}
	@Override public CursorPage<Enrollment> findByStudent(UUID id, Instant from, Instant to, CursorRequest request) {
		return query("enrollments-by-student", id.toString(), from, to, request);
	}
	@Override public CursorPage<Enrollment> findByCourse(UUID id, Instant from, Instant to, CursorRequest request) {
		return query("enrollments-by-course", id.toString(), from, to, request);
	}
	@Override public CursorPage<Enrollment> findByStatus(EnrollmentStatus status, CursorRequest request) {
		return query("enrollments-by-status", status.name(), null, null, request);
	}
	private CursorPage<Enrollment> query(String index, String partition, Instant from, Instant to, CursorRequest request) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new InvalidRequestException("enrolledFrom must not be after enrolledTo");
		}
		String lower = from == null ? null : DynamoSortKeys.timestampPrefix(from);
		String upper = to == null ? null : DynamoSortKeys.timestampPrefix(to) + "\uffff";
		var condition = lower != null && upper != null ? DynamoCursorQueries.between(partition, lower, upper)
				: lower != null ? DynamoCursorQueries.from(partition, lower)
				: upper != null ? DynamoCursorQueries.to(partition, upper)
				: DynamoCursorQueries.equalTo(partition);
		return DynamoCursorQueries.query(table().index(index), condition, request,
				DynamoCursorQueries.identity(table().tableName(), index, partition, lower, upper), cursorCodec,
				EnrollmentDynamoMapper::toDomain);
	}
}
