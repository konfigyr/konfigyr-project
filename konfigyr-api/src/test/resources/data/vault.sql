INSERT INTO vault_profiles(id, service_id, slug, name, description, state, policy, position, created_at, updated_at) VALUES
(1, 2, 'development', 'Development', NULL, 'ACTIVE', 'UNPROTECTED', 1, now() - interval '5 days', now() - interval '1 days'),
(2, 2, 'staging', 'Staging', NULL, 'ACTIVE', 'PROTECTED', 2, now() - interval '3 days', now() - interval '2 days'),
(3, 2, 'production', 'Prod', 'Careful!', 'ACTIVE', 'PROTECTED', 3, now() - interval '3 days', now() - interval '1 days'),
(4, 1, 'live', 'Production', 'Live configuration profile', 'ACTIVE', 'PROTECTED', 1, now() - interval '7 days', now() - interval '4 days');
