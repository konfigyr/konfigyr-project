-- This statement depends on namespaces.sql test data
INSERT INTO oauth_applications(id, namespace_id, name, client_id, client_secret, scopes, expires_at, created_at, updated_at) VALUES
(1, 2, 'Konfigyr expired app', 'kfg-A2c7mvoxEP1AW1BUqzQXbS3NAivjfAqD', '{noop}10S6cd0JgdO6WCLmOLB46d-Enx7K20hKSF1qicfev5g', 'namespaces', now() - interval '3 days', now() - interval '30 days', now() - interval '14 days'),
(2, 2, 'Konfigyr active app', 'kfg-A2c7mvoxEP1rb-_NQLvaZ5KJNTGR-oOp', '{noop}4b6dHEXXnAEMM1AD4b6RhqamjFwMdhIRgpyBVJRu-Zk', 'namespaces', now() + interval '3 days', now() - interval '7 days', now() - interval '1 days'),
(3, 1, 'Personal app', 'kfg-A2c7mvoxEP346BQCSuwnJ5ZNQIEsgCBG', '{argon2}$argon2id$v=19$m=19456,t=2,p=1$WWqVmXvRWs4N3jZznw22Pg$e3R152CcBvazCzghdqF92beaEdJMZ4iTZuQDV+GjxPw', 'namespaces', NULL, now() - interval '2 days', now() - interval '2 days');
