-- This statement depends on namespaces.sql test data
INSERT INTO namespace_members(id, namespace_id, account_id, role, since) VALUES
(1, 1, 1, 'ADMIN', now() - interval '5 days'),
(2, 2, 1, 'ADMIN', now() - interval '3 days'),
(3, 2, 2, 'USER', now() - interval '2 days');
