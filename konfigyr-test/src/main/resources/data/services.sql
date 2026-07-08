INSERT INTO services(id, namespace_id, slug, name, description, created_at, updated_at) VALUES
(1, 1, 'john-doe-blog', 'John Doe Blog', 'Personal John Doe blog application', now() - interval '5 days', now() - interval '1 days'),
(2, 2, 'konfigyr-id', 'Konfigyr ID', 'Konfigyr Identity service', now() - interval '3 days', now() - interval '1 days'),
(3, 2, 'konfigyr-api', 'Konfigyr API', 'Konfigyr REST API service', now() - interval '2 days', now() - interval '7 hours');

INSERT INTO service_releases(id, service_id, version, state, created_at) VALUES
(1, 2, 'latest', 'COMPLETE', now() - interval '3 days'),
(2, 3, 'latest', 'COMPLETE', now() - interval '2 days');

INSERT INTO service_artifacts(release_id, group_id, artifact_id, version, source, checksum, name, description, website, repository) VALUES
(1, 'org.springframework.boot', 'spring-boot', '4.0.4', 'ARTIFACTORY', NULL, NULL, NULL, NULL, NULL),
(1, 'org.springframework.boot', 'spring-boot-autoconfigure', '4.0.4', 'ARTIFACTORY', NULL, NULL, NULL, NULL, NULL),
(1, 'org.springframework.boot', 'spring-boot-actuator', '4.0.4', 'ARTIFACTORY', NULL, NULL, NULL, NULL, NULL),
(1, 'org.springframework.boot', 'spring-boot-jooq', '4.0.4', 'ARTIFACTORY', NULL, NULL, NULL, NULL, NULL),
(1, 'org.springframework.boot', 'spring-boot-liquibase', '4.0.4', 'ARTIFACTORY', NULL, NULL, NULL, NULL, NULL),
(1, 'org.springframework.modulith', 'spring-modulith-core', '2.0.3', 'ARTIFACTORY', NULL, NULL, NULL, NULL, NULL),
(1, 'org.springframework.modulith', 'spring-modulith-moments', '2.0.3', 'ARTIFACTORY', NULL, NULL, NULL, NULL, NULL),
(1, 'com.acme', 'spring-boot-service', '1.0.0', 'LOCAL', '6QRgbo04ZnpKhc3o5yZckptP+61bzEBhwNibipufooU=', 'Acme Spring Boot service', 'Spring Boot service', 'https://acme.com/service', 'https://github.com/acme/service'),
(2, 'com.konfigyr', 'konfigyr-crypto-api', '1.0.1', 'ARTIFACTORY', NULL, NULL, NULL, NULL, NULL),
(2, 'org.springframework.boot', 'spring-boot', '4.0.4', 'ARTIFACTORY', NULL, NULL, NULL, NULL, NULL),
(2, 'org.springframework.modulith', 'spring-modulith-core', '2.0.4', 'ARTIFACTORY', NULL, NULL, NULL, NULL, NULL),
(2, 'com.acme', 'spring-boot-library', '1.1.4', 'LOCAL', 'HX26naW4bSuS+0yUHCyXw83XsVgNAyafKDHD576DyhA=', 'Acme Spring Boot library', 'Spring Boot library', 'https://acme.com/library', 'https://github.com/acme/library');

INSERT INTO service_configuration_catalog(service_id, release_id, group_id, artifact_id, version, name, type_name, schema, description, default_value, deprecation) VALUES
(2, 1, 'org.springframework.boot', 'spring-boot', '4.0.3', 'spring.application.name', 'java.lang.String', '{"type":"string"}', 'Application name. Typically used with logging to help identify the application.', NULL, NULL),
(2, 1, 'org.springframework.boot', 'spring-boot', '4.0.3', 'spring.application.index', 'java.lang.Integer', '{"type":"integer","format":"int32"}', 'Application index.', NULL, NULL),
(2, 1, 'org.springframework.boot', 'spring-boot', '4.0.3', 'spring.application.deprecated', 'java.lang.Boolean', '{"type":"boolean"}', 'Deprecated property that is no longer needed.', 'true', '{"reason":"No longer needed"}'),
(2, 1, 'com.acme', 'spring-boot-service', '1.0.0', 'com.acme.service.property', 'java.lang.String', '{"type":"string"}', 'Local service property.', NULL, NULL),
(3, 2, 'com.acme', 'spring-boot-library', '1.1.4', 'com.acme.library.property', 'java.lang.Boolean', '{"type":"boolean"}', 'Local library property.', 'true', NULL),
(3, 2, 'com.acme', 'spring-boot-library', '1.1.4', 'com.acme.library.deprecated', 'java.lang.Integer', '{"type":"integer"}', 'Deprecated local library property.', '567889', '{"reason":"Removed without replacement"}');
