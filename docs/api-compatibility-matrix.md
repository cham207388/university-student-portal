# API compatibility matrix

The same controllers and DTOs serve both persistence profiles. Database-specific query implementations sit behind
datasource-neutral application query ports.

| Capability | DynamoDB behavior | PostgreSQL behavior | API impact | Migration decision |
|---|---|---|---|---|
| Resource CRUD | Conditional writes and transactional claims | JPA transactions, unique constraints, and optimistic versions | Same routes and DTOs | Preserve IDs, timestamps, and versions |
| Catalog pagination | Opaque LastEvaluatedKey cursor | Opaque, query-bound page cursor with stable UUID ordering | Same `limit`, `cursor`, `nextCursor`, and `hasNext` fields | Cursors are datasource-specific and cannot cross profiles |
| Exact alternate keys | GSI lookup | Unique-index repository lookup | Same exact filter behavior | Normalize values before either lookup |
| Relationship collections | GSI queries and batch hydration | Indexed joins with distinct logical rows | Same routes and envelopes | PostgreSQL removes physical edge-item concerns |
| Enrollment date filters | Enrollment GSIs | Indexed relational predicates | Same supported combinations | Preserve inclusive bounds |
| Enrollment capacity | DynamoDB transaction and counters | Pessimistic course-row lock and occupied-seat update | Same conflict result | PostgreSQL serializes contenders for a course |
| Active enrollment uniqueness | Transactional active lock | Partial unique index plus locked precheck | Same conflict result | Database constraint remains authoritative |
| Optimistic concurrency | Conditional version expressions | JPA versions plus explicit expected-version checks | Same stale-write conflict semantics | Never silently overwrite |
| Unsupported query parameters | Rejected before controller execution | Rejected before controller execution | Same RFC 9457 response | Keep strict contracts enabled in both profiles |
| Sorting | Access-pattern-defined DynamoDB order | Stable UUID order inside each query | Ordering may differ | Treat ordering and cursors as datasource-specific |

PostgreSQL cursors encode the query identity and page number but remain opaque to clients. Reusing a cursor with a
different filter is rejected. This preserves the public cursor envelope without pretending a DynamoDB key and a
relational page position are interchangeable.
