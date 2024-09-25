INSERT INTO repositories(id, namespace_id, slug, name, description, is_private, created_at, updated_at) VALUES
(1, 1, 'website', 'Johns Website', 'Personal website', true, now() - interval '3 days', now() - interval '1 days'),
(2, 2, 'konfigyr-crypto-api', 'Konfigyr Crypto API', 'Spring Boot Crypto API library', false, now() - interval '7 days', now() - interval '1 days'),
(3, 2, 'website', 'Konfigyr Website', 'Konfigyr Landing page', false, now() - interval '5 days', now() - interval '2 days'),
(4, 2, 'konfigyr-licences', 'Konfigyr Licences', 'Private repository', true, now() - interval '1 days', now() - interval '1 hours');
