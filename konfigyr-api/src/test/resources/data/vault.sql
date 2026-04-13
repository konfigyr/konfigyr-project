INSERT INTO vault_profiles(id, service_id, slug, name, description, state, policy, position, created_at, updated_at) VALUES
(1, 2, 'development', 'Development', NULL, 'ACTIVE', 'UNPROTECTED', 1, now() - interval '5 days', now() - interval '1 days'),
(2, 2, 'staging', 'Staging', NULL, 'ACTIVE', 'PROTECTED', 2, now() - interval '3 days', now() - interval '2 days'),
(3, 2, 'production', 'Prod', 'Careful!', 'ACTIVE', 'PROTECTED', 3, now() - interval '3 days', now() - interval '1 days'),
(4, 2, 'locked', 'QA', 'Locked profile', 'ACTIVE', 'IMMUTABLE', 4, now() - interval '17 days', now() - interval '9 days'),
(5, 1, 'live', 'Production', 'Live configuration profile', 'ACTIVE', 'PROTECTED', 1, now() - interval '7 days', now() - interval '4 days');

-- Create the partitions for change and property history for the previous two months
-- In total there should be 3 partitions per table (one for each month)
SELECT create_change_history_partition(now() - interval '1 month');
SELECT create_change_history_partition(now() - interval '2 month');
SELECT create_property_history_partition(now() - interval '1 month');
SELECT create_property_history_partition(now() - interval '2 month');

INSERT INTO vault_change_history(id, namespace_id, service_id, profile_id, revision, previous_revision, subject, description, change_count, author_id, author_type, author_name, created_at) VALUES
(1, 2, 2, 4, 'first-revision', NULL, 'First change', 'Initial changes', 1, 1, 'USER', 'John Doe', now() - interval '34 days'),
(2, 2, 2, 4, 'second-revision', 'first-revision', 'Second change', NULL, 2, 2, 'USER', 'Jane Doe', now() - interval '27 days'),
(3, 2, 2, 4, 'third-revision', 'second-revision', 'Third change', NULL, 1, 1, 'USER', 'John Doe', now() - interval '18 days'),
(4, 2, 2, 4, 'fourth-revision', 'third-revision', 'Fourth change', 'A while longer...', 1, 1, 'USER', 'John Doe', now() - interval '12 days'),
(5, 2, 2, 4, 'fifth-revision', 'fourth-revision', 'Fifth change', NULL, 1, 2, 'OAUTH_CLIENT', 'Konfigyr active app', now() - interval '9 days'),
(6, 2, 2, 4, 'sixth-revision', 'fifth-revision', 'Sixth change', 'The next one does it...', 1, 1, 'USER', 'John Doe', now() - interval '3 days'),
(7, 2, 2, 4, 'last-revision', 'sixth-revision', 'Last change', 'Got no more', 1, 1, 'USER', 'John Doe', now() - interval '2 days'),
(8, 2, 2, 3, 'first-revision', NULL, 'First change', 'Initial changes', 1, 1, 'USER', 'John Doe', now() - interval '2 days'),
(9, 1, 1, 5, 'first-revision', NULL, 'Testing', NULL, 1, 1, 'USER', 'John Doe', now() - interval '7 days');

INSERT INTO vault_property_history(change_id, profile_id, property_name, change_operation, new_value_checksum, new_value_cipher, old_value_checksum, old_value_cipher, created_at) VALUES
(1, 4, 'spring.application.name', 'ADDED', '1234567890', '1234567890', NULL, NULL, now() - interval '34 days'),
(2, 4, 'spring.application.name', 'UPDATED', '1234567890', '1234567890', '1234567890', '1234567890', now() - interval '27 days'),
(2, 4, 'spring.application.group', 'ADDED', '1234567890', '1234567890', NULL, NULL, now() - interval '27 days'),
(3, 4, 'spring.application.group', 'REMOVED', NULL, NULL, '1234567890', '1234567890', now() - interval '18 days'),
(4, 4, 'spring.application.name', 'UPDATED', '1234567890', '1234567890', '1234567890', '1234567890', now() - interval '12 days'),
(5, 4, 'spring.application.name', 'UPDATED', '1234567890', '1234567890', '1234567890', '1234567890', now() - interval '9 days'),
(6, 4, 'spring.application.name', 'UPDATED', '1234567890', '1234567890', '1234567890', '1234567890', now() - interval '3 days'),
(7, 4, 'spring.application.name', 'UPDATED', '1234567890', '1234567890', '1234567890', '1234567890', now() - interval '2 days'),
(9, 5, 'spring.application.name', 'ADDED', '1234567890', '1234567890', NULL, NULL, now() - interval '7 days');

INSERT INTO vault_change_requests(id, service_id, profile_id, number, state, merge_status, change_count, branch_name, base_revision, head_revision, subject, description, created_by, created_at, updated_at) VALUES
-- MERGED (clean flow)
(1, 2, 4, 1, 'MERGED', 'MERGEABLE', 1, 'refs/cr/1', 'rev-a1', 'rev-a2', 'Update application name', 'Align application name with new naming convention', 'John Doe', now() - interval '34 days', now() - interval '33 days'),
-- OPEN (approved, ready to merge)
(2, 2, 4, 2, 'OPEN', 'MERGEABLE', 2, 'refs/cr/2', 'rev-b1', 'rev-b2', 'Increase server port', 'Move service to new port range', 'John Doe', now() - interval '10 days', now() - interval '7 days'),
-- OPEN (changes requested)
(3, 2, 4, 3, 'OPEN', 'CHANGES_REQUESTED', 1, 'refs/cr/3', 'rev-c1', 'rev-c2', 'Update datasource URL', 'Point to staging database', 'John Doe', now() - interval '7 days', now() - interval '3 days'),
-- OPEN (outdated)
(4, 2, 3, 4, 'OPEN', 'OUTDATED', 1, 'refs/cr/4', 'rev-d1', 'rev-d2', 'Tune logging levels', 'Reduce log verbosity in production', 'Jane Doe', now() - interval '14 days', now() - interval '12 days'),
-- DISCARDED (closed without a merge)
(5, 1, 5, 1, 'DISCARDED', 'NOT_OPEN', 1, 'refs/cr/5', 'rev-a1', 'rev-a2', 'Experimental feature toggle', 'Testing feature toggle rollout', 'Jane Doe', now() - interval '20 days', now() - interval '14 days'),
-- DISCARDED (ghost CR, closed without a merge, any history or properties)
(6, 2, 2, 5, 'DISCARDED', 'NOT_OPEN', 1, 'refs/cr/6', 'rev-e1', 'rev-e2', 'Ghost change request', NULL, 'Jane Doe', now() - interval '9 days', now() - interval '2 days');

INSERT INTO vault_change_request_properties(change_request_id, property_name, change_operation, new_value_checksum, new_value_cipher, old_value_checksum, old_value_cipher) VALUES
-- CR 1 (merged)
(1, 'spring.application.name', 'UPDATED', 'chk-a-new', 'cipher-a-new', 'chk-a-old', 'cipher-a-old'),
-- CR 2 (approved, ready)
(2, 'server.port', 'UPDATED', 'chk-b-new', 'cipher-b-new', 'chk-b-old', 'cipher-b-old'),
(2, 'server.address', 'ADDED', 'chk-b-new', 'cipher-b-new', 'chk-b-old', 'cipher-b-old'),
-- CR 3 (changes requested)
(3, 'spring.datasource.url', 'UPDATED', 'chk-c-new', 'cipher-c-new', 'chk-c-old', 'cipher-c-old'),
-- CR 4 (outdated)
(4, 'logging.level.root', 'UPDATED', 'chk-d-new', 'cipher-d-new', 'chk-d-old', 'cipher-d-old'),
-- CR 5 (discarded)
(5, 'feature.toggle.experimental', 'CREATED', 'chk-e-new', 'cipher-e-new', NULL, NULL);

INSERT INTO vault_change_request_events(change_request_id, type, data, initiator, timestamp) VALUES
-- CR 1 (merged flow)
(1, 'CREATED', NULL, 'John Doe', now() - interval '34 days'),
(1, 'APPROVED', NULL, 'Jane Doe', now() - interval '33 days'),
(1, 'MERGED', NULL, 'John Doe', now() - interval '32 days'),
-- CR 2 (approved, still open)
(2, 'CREATED', NULL, 'John Doe', now() - interval '10 days'),
(2, 'APPROVED', NULL, 'Jane Doe', now() - interval '9 days'),
-- CR 3 (changes requested)
(3, 'CREATED', NULL, 'John Doe', now() - interval '7 days'),
(3, 'CHANGES_REQUESTED', NULL, 'Jane Doe', now() - interval '6 days'),
-- CR 4 (outdated, no further interaction)
(4, 'CREATED', NULL, 'Jane Doe', now() - interval '14 days'),
-- CR 5 (discarded)
(5, 'CREATED', NULL, 'Jane Doe', now() - interval '20 days'),
(5, 'COMMENTED', '{"comment":"Feature toggles are dropped."}', 'John Doe', now() - interval '19 days'),
(5, 'DISCARDED', NULL, 'Jane Doe', now() - interval '18 days');
