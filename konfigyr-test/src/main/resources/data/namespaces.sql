INSERT INTO namespaces(id, slug, name, description, created_at, updated_at) VALUES
(1, 'john-doe', 'John Doe', 'Personal namespace for John Doe', now() - interval '5 days', now() - interval '1 days'),
(2, 'konfigyr', 'Konfigyr', 'Konfigyr namespace', now() - interval '3 days', now() - interval '1 days');

INSERT INTO namespace_members(id, namespace_id, account_id, role, since) VALUES
(1, 1, 1, 'ADMIN', now() - interval '5 days'),
(2, 2, 1, 'ADMIN', now() - interval '3 days'),
(3, 2, 2, 'USER', now() - interval '2 days');

INSERT INTO oauth_applications(id, namespace_id, name, client_id, client_secret, scopes, expires_at, created_at, updated_at) VALUES
(1, 2, 'Konfigyr expired app', 'kfg-A2c7mvoxEP1AW1BUqzQXbS3NAivjfAqD', '{noop}10S6cd0JgdO6WCLmOLB46d-Enx7K20hKSF1qicfev5g', 'namespaces', now() - interval '3 days', now() - interval '30 days', now() - interval '14 days'),
(2, 2, 'Konfigyr active app', 'kfg-A2c7mvoxEP1rb-_NQLvaZ5KJNTGR-oOp', '{noop}4b6dHEXXnAEMM1AD4b6RhqamjFwMdhIRgpyBVJRu-Zk', 'namespaces', now() + interval '3 days', now() - interval '7 days', now() - interval '1 days'),
(3, 1, 'Personal app', 'kfg-A2c7mvoxEP346BQCSuwnJ5ZNQIEsgCBG', '{noop}n0obEPw2_5DoDNkxyXhW5Ul1TgC-t2r3H8_wj7PDqFc', 'namespaces', NULL, now() - interval '2 days', now() - interval '2 days');
