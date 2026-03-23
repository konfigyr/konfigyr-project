INSERT INTO artifacts(id, group_id, artifact_id, name, description, website, repository, created_at, updated_at) VALUES
(1, 'doe.john', 'website', 'Johns Website', 'Personal website', 'john.doe', NULL,now() - interval '3 days', now() - interval '1 days'),
(2, 'com.konfigyr', 'konfigyr-crypto-api', 'Konfigyr Crypto API', 'Spring Boot Crypto API library', NULL, 'https://github.com/konfigyr/konfigyr-crypto', now() - interval '7 days', now() - interval '1 days'),
(3, 'com.konfigyr', 'konfigyr-crypto-tink', 'Konfigyr Crypto Tink', 'Tink support Konfigyr Crypto API library', NULL, 'https://github.com/konfigyr/konfigyr-crypto', now() - interval '5 days', now() - interval '2 days'),
(4, 'com.konfigyr', 'konfigyr-licences', 'Konfigyr Licences', 'Private repository', NULL, NULL, now() - interval '1 days', now() - interval '1 hours'),
(5, 'com.konfigyr', 'konfigyr-api', 'Konfigyr API', 'Private REST API', 'konfigyr.api', 'https://github.com/konfigyr/konfigyr-project', now() - interval '1 days', now() - interval '1 hours'),
(6, 'org.springframework.boot', 'spring-boot', 'Spring Boot', 'Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications', 'https://spring.io/projects/spring-boot', 'https://github.com/spring-projects/spring-boot', now() - interval '6 days', now() - interval '1 days'),
(7, 'org.springframework.boot', 'spring-boot-autoconfigure', 'Spring Boot AutoConfigure', 'Spring Boot auto-configuration attempts to automatically configure your Spring applications', 'https://spring.io/projects/spring-boot', 'https://github.com/spring-projects/spring-boot', now() - interval '6 days', now() - interval '1 hours'),
(8, 'org.springframework.boot', 'spring-boot-actuator', 'Spring Boot Actuator', 'Spring Boot Actuator', 'https://spring.io/projects/spring-boot', 'https://github.com/spring-projects/spring-boot', now() - interval '6 days', now() - interval '1 hours'),
(9, 'org.springframework.modulith', 'spring-modulith-core', 'Spring Modulith Core', 'Modular monoliths with Spring Boot', 'https://spring.io/projects/spring-modulith/spring-modulith-core', 'https://github.com/spring-projects-experimental/spring-modulith', now() - interval '10 days', now() - interval '9 days'),
(10, 'org.springframework.modulith', 'spring-modulith-moments', 'Spring Modulith Moments', 'Modular monoliths with Spring Boot', 'https://spring.io/projects/spring-modulith/spring-modulith-moments', 'https://github.com/spring-projects-experimental/spring-modulith', now() - interval '10 days', now() - interval '9 days');

INSERT INTO artifact_versions(id, artifact_id, version, state, checksum, released_at) VALUES
(1, 1, '1.0.0', 'RELEASED', decode('lmUgkxFN4ru/3vuoTkcrYdf2Z5IfcSnmmtfnZJh+Lwo=', 'base64'), now() - interval '1 days'),
(2, 2, '1.0.0', 'RELEASED', decode('/dDqvCohLTXiFVbzK9EPgoFqx7LMj/PHtd/5ycpeV8s=', 'base64'), now() - interval '7 days'),
(3, 2, '1.0.1', 'RELEASED', decode('tBFMF6xGmSUIYo1skF45tzoCCN9WN026PdxUUBb4P7c=', 'base64'), now() - interval '3 days'),
(4, 2, '1.0.2', 'RELEASED', decode('Z4ZfvHMxf2DwgPPzOpWR/F19myuTCvYVVN7WSf3vxTs=', 'base64'), now() - interval '1 days'),
(5, 3, '1.0.0', 'RELEASED', decode('xWl6bKMwnQ1ZVs/CVKuoFufZACtb7oh5uebZr0T6txA=', 'base64'), now() - interval '2 days'),
(6, 5, '1.0.0', 'RELEASED', decode('7FTrQ6LxfT/s9QYsmHx5TqAl2iWN4LbqZINULveeP4o=', 'base64'), now() - interval '1 hours'),
(7, 6, '4.0.4', 'RELEASED', decode('7FTrQ6LxfT/s9QYsmHx5TqAl2iWN4LbqZINULveeP4o=', 'base64'), now() - interval '6 hours'),
(8, 7, '4.0.4', 'RELEASED', decode('7FTrQ6LxfT/s9QYsmHx5TqAl2iWN4LbqZINULveeP4o=', 'base64'), now() - interval '3 days'),
(9, 8, '4.0.4', 'RELEASED', decode('7FTrQ6LxfT/s9QYsmHx5TqAl2iWN4LbqZINULveeP4o=', 'base64'), now() - interval '2 days'),
(10, 9, '2.0.4', 'RELEASED', decode('7FTrQ6LxfT/s9QYsmHx5TqAl2iWN4LbqZINULveeP4o=', 'base64'), now() - interval '10 days'),
(11, 10, '2.0.4', 'RELEASED', decode('7FTrQ6LxfT/s9QYsmHx5TqAl2iWN4LbqZINULveeP4o=', 'base64'), now() - interval '10 days');

INSERT INTO property_definitions(id, artifact_id, checksum, name, type_name, schema, default_value, description, deprecation, occurrences, first_seen, last_seen) VALUES
(1, 2, decode('cRJ8jlPpTPmTJEoZEZNDSjvdqafG05QkzNJplXyu9J0=', 'base64'), 'spring.application.name', 'java.lang.String', convert_to('{"type":"string"}', 'UTF-8'), NULL, 'Application name. Typically used with logging to help identify the application.', NULL, 1, '1.0.1', '1.0.1'),
/* Spring core properties */
(2, 6, decode('v008AY5CYkzDGvxlH1E8A2sSR5EDg6UDem5jE+ZpawA=', 'base64'), 'spring.profiles.active', 'java.util.List<java.lang.String>', convert_to('{"items":{"type":"string"},"type":"array"}', 'UTF-8'), NULL, 'Comma-separated list of active profiles. Can be overridden by a command line switch.', NULL, 1, '4.0.4', '4.0.4'),
(3, 6, decode('TbtNm6N3iKoSWxA3WvOP+Ojsh6BJck6Tlcq8LCK30is=', 'base64'), 'spring.profiles.default', 'java.lang.String', convert_to('{"type":"string"}', 'UTF-8'), 'default', 'Name of the profile to enable if no profile is active.', NULL, 1, '4.0.4', '4.0.4'),
(4, 6, decode('mX5lIBDYqbBOQzCsVTNop78aMicMePNgraR7/hWl2SE=', 'base64'), 'spring.profiles.validate', 'java.lang.Boolean', convert_to('{"type":"boolean"}', 'UTF-8'), 'true', 'Whether profiles should be validated to ensure sensible names are used.', NULL, 1, '4.0.4', '4.0.4'),
/* Spring autoconfigure properties */
(5, 7, decode('3Jas0oDf1TLJDn2fInLdGJfhGf/Vwa2q8t4PQ0CFA3A=', 'base64'), 'spring.messages.basename', 'java.util.List<java.lang.String>', convert_to('{"items":{"type":"string"},"type":"array"}', 'UTF-8'), 'messages', 'List of basenames (essentially a fully-qualified classpath location), each following the ResourceBundle convention with relaxed support for slash based locations. If it doesn''t contain a package qualifier (such as "org.mypackage"), it will be resolved from the classpath root.', NULL, 1, '4.0.4', '4.0.4'),
(6, 7, decode('L00K3chGss7gZbW+gC7ONlfarxA2JoYoUo0yHkhvuCI=', 'base64'), 'spring.messages.cache-duration', 'java.time.Duration', convert_to('{"format":"duration","type":"string"}', 'UTF-8'), NULL, 'Loaded resource bundle files cache duration. When not set, bundles are cached forever. If a duration suffix is not specified, seconds will be used.', NULL, 1, '4.0.4', '4.0.4'),
(7, 7, decode('/+KM6drA4mhxFk1r4s2whoBSgfFWi42VZ3uB+Ulz8oA=', 'base64'), 'spring.messages.encoding', 'java.nio.charset.Charset', convert_to('{"format":"charset","type":"string"}', 'UTF-8'), 'UTF-8', 'Message bundles encoding.', NULL, 1, '4.0.4', '4.0.4'),
/* Spring actuator properties */
(8, 8, decode('fIMnLcXBeBIKN3pikyFVZcrdrZ9RGwpju5Rypa79vC4=', 'base64'), 'management.endpoint.startup.access', 'org.springframework.boot.actuate.endpoint.Access', convert_to('{"enum":["NONE","READ_ONLY","UNRESTRICTED"],"type":"string"}', 'UTF-8'), 'unrestricted', 'Permitted level of access for the startup endpoint.', NULL, 1, '4.0.4', '4.0.4'),
(9, 8, decode('3xQTFMSu00xtzQ/yslStqzCB1eXp3smbKZ3vetqbn0s=', 'base64'), 'management.endpoint.shutdown.access', 'org.springframework.boot.actuate.endpoint.Access', convert_to('{"enum":["NONE","READ_ONLY","UNRESTRICTED"],"type":"string"}', 'UTF-8'), 'none', 'Permitted level of access for the shutdown endpoint.', NULL, 1, '4.0.4', '4.0.4'),
(10, 8, decode('Lgc2cY3QFRNfFWrupXheplV0/X4zz24F7jvduEGBX3A=', 'base64'), 'management.endpoint.sbom.access', 'org.springframework.boot.actuate.endpoint.Access', convert_to('{"enum":["NONE","READ_ONLY","UNRESTRICTED"],"type":"string"}', 'UTF-8'), 'unrestricted', 'Permitted level of access for the sbom endpoint.', NULL, 1, '4.0.4', '4.0.4'),
(11, 8, decode('gGZz9BhrwVM243OLfIbM8cxKHO1oWwESSQRjw8DYFm0=', 'base64'), 'management.endpoint.sbom.application.media-type', 'org.springframework.util.MimeType', convert_to('{"format":"mime-type","type":"string"}', 'UTF-8'), NULL, 'Media type of the SBOM. If null, the media type will be auto-detected.', NULL, 1, '4.0.4', '4.0.4'),
/* Spring modulith properties */
(12, 9, decode('rFpxmzVZOb1AkNgLifaEb1et+ZIwlmrnjwGX4smxFAA=', 'base64'), 'spring.modulith.detection-strategy', 'java.lang.String', convert_to('{"examples":["direct-sub-packages","explicitly-annotated"],"type":"string"}', 'UTF-8'), NULL, 'The strategy how to detect application modules.', NULL, 1, '2.0.4', '2.0.4'),
(13, 10, decode('vfWkFvWqOO2AH/j1J1L/8q1t0XDR5DE6bkGsGWlpXrs=', 'base64'), 'spring.modulith.moments.enable-time-machine', 'java.lang.Boolean', convert_to('{"type":"boolean"}', 'UTF-8'), 'false', NULL, NULL, 1, '2.0.4', '2.0.4'),
(14, 10, decode('SMWccCBOl+JJOAD028wbsyESb+QscbuqbqF1Xm270Qk=', 'base64'), 'spring.modulith.moments.granularity', 'org.springframework.modulith.moments.support.MomentsProperties$Granularity', convert_to('{"enum":["DAYS","HOURS"],"type":"string"}', 'UTF-8'), 'hours', NULL, NULL, 1, '2.0.4', '2.0.4'),
(15, 10, decode('sPa3aP2xi0pWOOqKc1o2BnAO5Vi2n/7CNwopYSmucIs=', 'base64'), 'spring.modulith.moments.locale', 'java.util.Locale', convert_to('{"format":"language","type":"string"}', 'UTF-8'), NULL, NULL, NULL, 1, '2.0.4', '2.0.4'),
(16, 10, decode('tTlj+7N0mkLnn4uYgrmK4XDaCz72NCLR3z23D/DiTtc=', 'base64'), 'spring.modulith.moments.zone-id', 'java.time.ZoneId', convert_to('{"format":"time-zone","type":"string"}', 'UTF-8'), NULL, NULL, NULL, 1, '2.0.4', '2.0.4');

INSERT INTO artifact_version_properties(artifact_version_id, property_definition_id) VALUES
(3, 1),
/* Spring core 4.0.4 properties */
(7, 2),
(7, 3),
(7, 4),
/* Spring autoconfigure 4.0.4 properties */
(8, 5),
(8, 6),
(8, 7),
/* Spring actuator 4.0.4 properties */
(9, 8),
(9, 9),
(9, 10),
(9, 11),
/* Spring modulith core 2.0.4 properties */
(10, 12),
/* Spring modulith moments 2.0.4 properties */
(11, 13),
(11, 14),
(11, 15),
(11, 16);

/** updates the auto-increment sequences to start at 1000 **/
ALTER SEQUENCE artifacts_id_seq RESTART with 1000;
ALTER SEQUENCE artifact_versions_id_seq RESTART with 1000;
ALTER SEQUENCE property_definitions_id_seq RESTART with 1000;
