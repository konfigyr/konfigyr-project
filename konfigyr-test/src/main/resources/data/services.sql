INSERT INTO services(id, namespace_id, slug, name, description, created_at, updated_at) VALUES
(1, 1, 'john-doe-blog', 'John Doe Blog', 'Personal John Doe blog application', now() - interval '5 days', now() - interval '1 days'),
(2, 2, 'konfigyr-id', 'Konfigyr ID', 'Konfigyr Identity service', now() - interval '3 days', now() - interval '1 days'),
(3, 2, 'konfigyr-api', 'Konfigyr API', 'Konfigyr REST API service', now() - interval '2 days', now() - interval '7 hours');
