/* owned by an uninvolved namespace (ebf) under the same groupId as the reclaimed-artifact fixture (see konfigyr-test's artifactory.sql): used to verify that accepting an ownership transfer only moves the requested 'from' namespace's artifacts, leaving other owners under that groupId untouched */
INSERT INTO artifacts(id, namespace_id, group_id, artifact_id, visibility, name, description, website, repository, created_at, updated_at) VALUES
(16, 3, 'com.konfigyr', 'ebf-artifact', 'PRIVATE', 'EBF Artifact', 'Owned by the ebf namespace, uninvolved in any ownership transfer request', NULL, NULL, now() - interval '1 days', now() - interval '1 days'),
/* owned by ebf under a groupId nobody has an active claim on yet: used to verify that claiming or verifying a fresh groupId surfaces this pre-existing owner as a conflictingOwner */
(17, 3, 'com.acme.widgets', 'widget-lib', 'PRIVATE', 'Widget Lib', 'Owned by ebf under a groupId nobody has claimed yet', NULL, NULL, now() - interval '10 days', now() - interval '10 days');

/* one transfer request per TransferState, using groupId/namespace combinations that the dynamic test scenarios never request, so they can't collide with the partial unique index on (group_id, from_namespace_id, to_namespace_id) WHERE state = 'PENDING' */
INSERT INTO artifact_ownership_transfers(id, group_id, from_namespace_id, to_namespace_id, state, requested_at, resolved_at) VALUES
(1, 'com.acme.billing', 2, 3, 'PENDING', now() - interval '1 days', NULL),
(2, 'com.acme.reporting', 1, 2, 'ACCEPTED', now() - interval '5 days', now() - interval '4 days'),
(3, 'com.acme.search', 2, 1, 'REJECTED', now() - interval '6 days', now() - interval '5 days'),
(4, 'com.acme.payments', 1, 3, 'CANCELLED', now() - interval '3 days', now() - interval '2 days');
