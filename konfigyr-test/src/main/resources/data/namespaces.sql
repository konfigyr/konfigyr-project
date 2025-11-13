INSERT INTO namespaces(id, slug, name, description, created_at, updated_at) VALUES
(1, 'john-doe', 'John Doe', 'Personal namespace for John Doe', now() - interval '5 days', now() - interval '1 days'),
(2, 'konfigyr', 'Konfigyr', 'Konfigyr namespace', now() - interval '3 days', now() - interval '1 days');
