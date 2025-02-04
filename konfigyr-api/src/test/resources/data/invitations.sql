INSERT INTO invitations(key, namespace_id, sender_id, recipient_id, recipient_email, role, expiry_date) VALUES
('09320d7f8e21143b2957f1caded74cbc', 2, 1, NULL, 'invitee@konfigyr.com', 'ADMIN', CURRENT_TIMESTAMP + INTERVAL '7 DAYS'),
('09320f6c6481c1fed73573a5430758f1', 2, NULL, NULL, 'expiring@konfigyr.com', 'USER', CURRENT_TIMESTAMP - INTERVAL '10 DAYS');
