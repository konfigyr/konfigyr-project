-- This statement depends on namespaces.sql test data
INSERT INTO namespace_trusted_issuers(id, namespace_id, name, description, issuer_uri, jwks_uri, is_active, allowed_audiences, custom_claims, created_at, updated_at) VALUES
(1, 2, 'Konfigyr CI', 'GitHub Actions for Konfigyr org', 'https://ci.konfigyr.com', 'https://ci.konfigyr.com/jwks.json', true,  '["konfigyr-api"]', '{"environment":"production"}', now() - interval '10 days', now() - interval '2 days'),
(2, 2, 'Konfigyr staging CI', NULL, 'https://ci-staging.konfigyr.com', NULL, true, '[]', '{}', now() - interval '5 days', now() - interval '5 days'),
(3, 2, 'Disabled issuer', 'This issuer is inactive', 'https://disabled.konfigyr.com', NULL, false, '[]', '{}', now() - interval '7 days', now() - interval '7 days'),
(4, 1, 'Personal CI', NULL, 'https://ci.john-doe.example.com', NULL, true, '[]', '{}', now() - interval '2 days', now() - interval '2 days');
