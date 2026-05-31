INSERT INTO KEYSETS (KEYSET_NAME, KEYSET_PURPOSE, KEYSET_FACTORY, KEYSET_VERSION, KEYSET_PROVIDER, KEYSET_KEK, ROTATION_INTERVAL, DESTRUCTION_GRACE_PERIOD)
VALUES
    ('kms-john-doe-keyset',    'ENCRYPTION', 'tink', 0, 'konfigyr-registry', 'master', 31536000000, 2592000000),
    ('kms-john-doe-signing',   'SIGNING',    'tink', 0, 'konfigyr-registry', 'master', 31536000000, 2592000000),
    ('kms-konfigyr-active',    'ENCRYPTION', 'tink', 0, 'konfigyr-registry', 'master', 31536000000, 2592000000),
    ('kms-konfigyr-inactive',  'SIGNING',    'tink', 0, 'konfigyr-registry', 'master', 31536000000, 2592000000),
    ('kms-konfigyr-deleted',   'SIGNING',    'tink', 0, 'konfigyr-registry', 'master', 31536000000, 2592000000),
    ('kms-konfigyr-destroyed', 'ENCRYPTION', 'tink', 0, 'konfigyr-registry', 'master', 31536000000, 2592000000);

INSERT INTO KEYSET_KEYS (KEYSET_NAME, KEY_ID, KEY_ALGORITHM, KEY_TYPE, KEY_STATUS, KEY_PRIMARY, KEY_DATA, CREATED_AT, INITIALIZED_AT, EXPIRES_AT, DESTRUCTION_SCHEDULED_AT, DESTROYED_AT)
VALUES
    (
        'kms-john-doe-keyset',
        '106475',
        'tink:AES128_GCM', 'OCTET', 'ENABLED', true,
        decode('Zum85eP4nvjCkbMmgwOmIdGUSIUChYSi8u7U7u0aoHnwiEYl2bJta2TgqZuZSdTbiHkuToduf46L4hsoqJtB9Rfysd0Jbdd7JlM471oqGxsrNIp60sj2YwfOpi72eezmAf7XSg==', 'base64'),
        (extract(epoch from now() - interval '3 days') * 1000)::bigint,
        (extract(epoch from now() - interval '3 days') * 1000)::bigint,
        (extract(epoch from now() + interval '87 days') * 1000)::bigint,
        NULL, NULL
    ),
    (
        'kms-john-doe-keyset',
        '407725',
        'tink:AES128_GCM', 'OCTET', 'ENABLED', false,
        decode('Lsxu6hSYgJBHs/tAC5dc3HSBUrmCjR1ZaJFalEBSi9JVAJGkyXSwYA5zb8VUTpa5K3UyqBP8Y2cgUSWdNE8q6VxGZJ3cJybFutGPQiz8I9MxG+M3nOvi2cXgTQVe/W1J+oV2Kw==', 'base64'),
        (extract(epoch from now() - interval '30 days') * 1000)::bigint,
        (extract(epoch from now() - interval '30 days') * 1000)::bigint,
        (extract(epoch from now() + interval '60 days') * 1000)::bigint,
        NULL, NULL
    ),
    (
        'kms-john-doe-signing',
        '738802',
        'tink:ECDSA_P256', 'EC', 'ENABLED', true,
        decode('8DCWORAE44LMZht5y2ktRalBmYaGHOFLLsDB2EEl9eagqLGn9TiYNJXC6K0e4Rsj1OSp4t+Dmfr8FBZnRuuroc1/TPpnNcHrjuHtbKr3zBuAEonMI8wMNPGSKztggk4vj1H7BuxwIfbjNUVaRU5DDpP6/UPuTLMqSCw5aqGwshaTnEEayKBQ4arAr2ZC5Migiue9DPYkgNSS4lsC+8QUTrLokC7z59/3OWR78p9opkPXLE3mq8A+kAwxET4KnWHaQWrLa9xgAlSE9Jo=', 'base64'),
        (extract(epoch from now() - interval '1 days') * 1000)::bigint,
        (extract(epoch from now() - interval '1 days') * 1000)::bigint,
        (extract(epoch from now() + interval '89 days') * 1000)::bigint,
        NULL, NULL
    ),
    (
        'kms-konfigyr-active',
        '738802',
        'tink:AES256_GCM', 'OCTET', 'ENABLED', true,
        decode('tNtuoyY6WJUKDq0zmH2QVyDZthtMHTtlZNgRlnsTlYC+NFtjEiU2ZbrzemUZ+CYURY8dC7QgUkt3WFgQ0AH6yfBhN6vLLSfL9NlSs7TgPJrfWZZHldAJl/NYcbmpQlSjwvkF3gNLMzTNuKGABGTGVTw+Nq4=', 'base64'),
        (extract(epoch from now() - interval '1 days') * 1000)::bigint,
        (extract(epoch from now() - interval '1 days') * 1000)::bigint,
        (extract(epoch from now() + interval '89 days') * 1000)::bigint,
        NULL, NULL
    ),
    (
        'kms-konfigyr-active',
        '604025',
        'tink:AES128_GCM', 'OCTET', 'ENABLED', false,
        decode('Lsxu6hSYgJBHs/tAC5dc3HSBUrmCjR1ZaJFalEBSi9JVAJGkyXSwYA5zb8VUTpa5K3UyqBP8Y2cgUSWdNE8q6VxGZJ3cJybFutGPQiz8I9MxG+M3nOvi2cXgTQVe/W1J+oV2Kw==', 'base64'),
        (extract(epoch from now() - interval '7 days') * 1000)::bigint,
        (extract(epoch from now() - interval '7 days') * 1000)::bigint,
        (extract(epoch from now() + interval '83 days') * 1000)::bigint,
        NULL, NULL
    ),
    (
        'kms-konfigyr-inactive',
        '374108',
        'tink:ECDSA_P256', 'EC', 'DISABLED', true,
        decode('8DCWORAE44LMZht5y2ktRalBmYaGHOFLLsDB2EEl9eagqLGn9TiYNJXC6K0e4Rsj1OSp4t+Dmfr8FBZnRuuroc1/TPpnNcHrjuHtbKr3zBuAEonMI8wMNPGSKztggk4vj1H7BuxwIfbjNUVaRU5DDpP6/UPuTLMqSCw5aqGwshaTnEEayKBQ4arAr2ZC5Migiue9DPYkgNSS4lsC+8QUTrLokC7z59/3OWR78p9opkPXLE3mq8A+kAwxET4KnWHaQWrLa9xgAlSE9Jo=', 'base64'),
        (extract(epoch from now() - interval '30 days') * 1000)::bigint,
        (extract(epoch from now() - interval '30 days') * 1000)::bigint,
        NULL, NULL, NULL
    ),
    (
        'kms-konfigyr-deleted',
        '162009',
        'tink:ED25519', 'EC', 'PENDING_DESTRUCTION', true,
        decode('hGcL0wQd2hHZzW4LHV8LJqkB5Jc8+Ooq9Mlx51qhDW9cHERiSTrZJBfWyoq876+dL+iW++T6HZvULWJKBz+T4e3H0TMm6lTVXhFH8JXsJ27cJyiRSeUCeY+BTinpmZ6CbMToLxC55AB64gdI9OLNPQNP8S2M1oHXWfMkkVJFBzol54JEusXRZ3qLXbFB4S3R2N9bzkH94jAXnqfjibbiqA==', 'base64'),
        (extract(epoch from now() - interval '30 days') * 1000)::bigint,
        (extract(epoch from now() - interval '30 days') * 1000)::bigint,
        NULL,
        (extract(epoch from now() + interval '8 hours') * 1000)::bigint,
        NULL
    ),
    (
        'kms-konfigyr-destroyed',
        '431904',
        'tink:AES128_GCM', 'OCTET', 'DESTROYED', true,
        decode('Zum85eP4nvjCkbMmgwOmIdGUSIUChYSi8u7U7u0aoHnwiEYl2bJta2TgqZuZSdTbiHkuToduf46L4hsoqJtB9Rfysd0Jbdd7JlM471oqGxsrNIp60sj2YwfOpi72eezmAf7XSg==', 'base64'),
        (extract(epoch from now() - interval '10 days') * 1000)::bigint,
        (extract(epoch from now() - interval '10 days') * 1000)::bigint,
        NULL,
        (extract(epoch from now() - interval '8 days') * 1000)::bigint,
        (extract(epoch from now() - interval '8 days') * 1000)::bigint
    );

INSERT INTO kms_keyset_metadata(id, namespace_id, keyset_id, name, description, tags, created_at, updated_at, destroyed_at) VALUES
(1, 1, 'kms-john-doe-keyset', 'john-doe-keyset', 'John Doe keyset', 'private-key', now() - interval '3 days', now() - interval '1 days', NULL),
(2, 2, 'kms-konfigyr-active', 'konfigyr-active', 'Active keyset', 'encryption,konfigyr', now() - interval '7 days', now() - interval '3 days', NULL),
(3, 2, 'kms-konfigyr-inactive', 'konfigyr-inactive', 'Inactive keyset', 'signing,konfigyr', now() - interval '30 days', now() - interval '18 days', NULL),
(4, 2, 'kms-konfigyr-deleted', 'konfigyr-deleted', 'Pending removal keyset', NULL, now() - interval '30 days', now() - interval '18 days', NULL),
(5, 2, 'kms-konfigyr-destroyed', 'konfigyr-destroyed', 'Destroyed keyset', 'destroyed,konfigyr', now() - interval '10 days', now() - interval '8 days', now() - interval '7 days'),
(6, 1, 'kms-john-doe-signing', 'signing keyset', NULL, NULL, now() - interval '1 days', now() - interval '8 hours', NULL);
