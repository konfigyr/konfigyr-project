INSERT INTO namespaces(id, slug, name, description, created_at, updated_at) VALUES
(1, 'john-doe', 'John Doe', 'Personal namespace for John Doe', now() - interval '5 days', now() - interval '1 days'),
(2, 'konfigyr', 'Konfigyr', 'Konfigyr namespace', now() - interval '3 days', now() - interval '1 days');

INSERT INTO namespace_members(id, namespace_id, account_id, role, since) VALUES
(1, 1, 1, 'ADMIN', now() - interval '5 days'),
(2, 2, 1, 'ADMIN', now() - interval '3 days'),
(3, 2, 2, 'USER', now() - interval '2 days');
