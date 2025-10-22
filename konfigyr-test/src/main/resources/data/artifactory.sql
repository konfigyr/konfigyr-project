INSERT INTO artifacts(id, group_id, artifact_id, name, description, website, repository, created_at, updated_at) VALUES
(1, 'doe.john', 'website', 'Johns Website', 'Personal website', 'john.doe', NULL,now() - interval '3 days', now() - interval '1 days'),
(2, 'com.konfigyr', 'konfigyr-crypto-api', 'Konfigyr Crypto API', 'Spring Boot Crypto API library', NULL, 'https://github.com/konfigyr/konfigyr-crypto', now() - interval '7 days', now() - interval '1 days'),
(3, 'com.konfigyr', 'konfigyr-crypto-tink', 'Konfigyr Crypto Tink', 'Tink support Konfigyr Crypto API library', NULL, 'https://github.com/konfigyr/konfigyr-crypto', now() - interval '5 days', now() - interval '2 days'),
(4, 'com.konfigyr', 'konfigyr-licences', 'Konfigyr Licences', 'Private repository', NULL, NULL, now() - interval '1 days', now() - interval '1 hours'),
(5, 'com.konfigyr', 'konfigyr-api', 'Konfigyr API', 'Private REST API', 'konfigyr.api', 'https://github.com/konfigyr/konfigyr-project', now() - interval '1 days', now() - interval '1 hours');

INSERT INTO artifact_versions(id, artifact_id, version, released_at) VALUES
(1, 1, '1.0.0', now() - interval '1 days'),
(2, 2, '1.0.0', now() - interval '7 days'),
(3, 2, '1.0.1', now() - interval '3 days'),
(4, 2, '1.0.2', now() - interval '1 days'),
(5, 3, '1.0.0', now() - interval '2 days'),
(6, 5, '1.0.0', now() - interval '1 hours');

INSERT INTO property_definitions(id, artifact_id, checksum, name, data_type, type, type_name, default_value, description, hints, deprecation, occurrences, first_seen, last_seen) VALUES
(1, 2, decode('8IiKOly5JR3uQJoeTBFU7BRkX7enEjgG+XwqPEv3lAo=', 'base64'), 'spring.application.name', 'ATOMIC', 'STRING', 'java.lang.String', NULL, 'Application name. Typically used with logging to help identify the application.', NULL, NULL, 1, '1.0.1', '1.0.1');

INSERT INTO artifact_version_properties(artifact_version_id, property_definition_id) VALUES
(3, 1);

/** updates the auto-increment sequences to start at 1000 **/
ALTER SEQUENCE artifacts_id_seq RESTART with 1000;
ALTER SEQUENCE artifact_versions_id_seq RESTART with 1000;
ALTER SEQUENCE property_definitions_id_seq RESTART with 1000;
