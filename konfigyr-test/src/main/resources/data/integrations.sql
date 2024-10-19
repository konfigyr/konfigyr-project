INSERT INTO integrations(id, namespace_id, type, provider, provider_reference, created_at, updated_at) VALUES
(1, 1, 'SOURCE_CODE', 'GITHUB', '110011', now() - interval '5 days', now() - interval '1 days'),
(2, 2, 'SOURCE_CODE', 'GITHUB', '220022', now() - interval '1 days', now() - interval '1 days');
