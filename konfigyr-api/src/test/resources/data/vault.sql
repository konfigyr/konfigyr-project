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
