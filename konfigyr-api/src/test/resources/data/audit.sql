-- Create audit_events partitions for the current and previous two months
SELECT create_audit_events_partition(now());
SELECT create_audit_events_partition(now() - interval '1 month');
SELECT create_audit_events_partition(now() - interval '2 month');

INSERT INTO audit_events(id, namespace_id, entity_type, entity_id, event_type, actor_id, actor_type, actor_name, details, created_at) VALUES
-- Namespace events for konfigyr (namespace_id = 2)
('019690a1-b4a0-7000-8000-000000000003', 2, 'namespace', 2, 'namespace.renamed', 'john.doe@konfigyr.com', 'user', 'John Doe', '{"oldName": "Konfig", "newName": "Konfigyr", "@class": "java.util.Collections$UnmodifiableMap"}', now() - interval '30 days'),
('019690a1-b4a0-7000-8000-000000000004', 2, 'namespace', 2, 'namespace.updated', 'jane.doe@konfigyr.com', 'user', 'Jane Doe', '{"field": "description", "@class": "java.util.Collections$UnmodifiableMap"}', now() - interval '25 days'),
('019690a1-b4a0-7000-8000-000000000012', 2, 'namespace', 2, 'namespace.member.added', 'john.doe@konfigyr.com', 'user', 'John Doe', '{"member": "invitee@konfigyr.com", "@class": "java.util.Collections$UnmodifiableMap"}', now() - interval '2 days'),
('019690a1-b4a0-7000-8000-000000000013', 2, 'namespace', 2, 'namespace.member.removed', 'jane.doe@konfigyr.com', 'user', 'Jane Doe', '{"member": "removed@konfigyr.com", "@class": "java.util.Collections$UnmodifiableMap"}', now() - interval '1 day'),
-- Service events for konfigyr namespace
('019690a1-b4a0-7000-8000-000000000005', 2, 'service', 2, 'service.created', 'john.doe@konfigyr.com', 'user', 'John Doe', '{"name": "config-service", "@class": "java.util.Collections$UnmodifiableMap"}', now() - interval '20 days'),
('019690a1-b4a0-7000-8000-000000000006', 2, 'service', 2, 'service.updated', 'jane.doe@konfigyr.com', 'user', 'Jane Doe', NULL, now() - interval '15 days'),
-- KMS events
('019690a1-b4a0-7000-8000-000000000007', 2, 'kms', 1, 'kms.key.rotated', 'system', 'system', 'System', '{"algorithm": "AES-256", "@class": "java.util.Collections$UnmodifiableMap"}', now() - interval '10 days'),
-- Invitation events
('019690a1-b4a0-7000-8000-000000000008', 2, 'invitation', 1, 'invitation.sent', 'john.doe@konfigyr.com', 'user', 'John Doe', '{"email": "invitee@konfigyr.com", "role": "ADMIN", "@class": "java.util.Collections$UnmodifiableMap"}', now() - interval '8 days'),
('019690a1-b4a0-7000-8000-000000000009', 2, 'invitation', 1, 'invitation.accepted', 'invitee@konfigyr.com', 'user', 'Invitee', NULL, now() - interval '7 days'),
-- Account events (no namespace context)
('019690a1-b4a0-7000-8000-000000000001', NULL, 'account', 1, 'account.created', 'john.doe@konfigyr.com', 'user', 'John Doe', NULL, now() - interval '35 days'),
('019690a1-b4a0-7000-8000-000000000002', NULL, 'account', 2, 'account.created', 'jane.doe@konfigyr.com', 'user', 'Jane Doe', NULL, now() - interval '34 days'),
-- Events for personal namespace (namespace_id = 1)
('019690a1-b4a0-7000-8000-000000000010', 1, 'service', 1, 'service.created', 'john.doe@konfigyr.com', 'user', 'John Doe', '{"name": "my-service", "@class": "java.util.Collections$UnmodifiableMap"}', now() - interval '5 days'),
-- OAuth client event
('019690a1-b4a0-7000-8000-000000000011', 2, 'service', 2, 'service.config.pushed', 'konfigyr-app', 'oauth_client', 'Konfigyr Active App', '{"revision": "r42", "@class": "java.util.Collections$UnmodifiableMap"}', now() - interval '3 days');
