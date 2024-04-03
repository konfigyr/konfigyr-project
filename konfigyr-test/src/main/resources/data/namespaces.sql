INSERT INTO namespaces(id, owner, slug, type, name, description, created_at, updated_at) VALUES
(1, 1, 'john-doe', 'PERSONAL', 'John Doe', 'Personal namespace for John Doe', now() - interval '5 days', now() - interval '1 days'),
(2, 1, 'konfigyr', 'TEAM', 'Konfigyr', 'Konfigyr namespace', now() - interval '3 days', now() - interval '1 days');

