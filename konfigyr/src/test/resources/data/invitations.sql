INSERT INTO invitations(key, namespace_id, sender_id, recipient_id, recipient_email, role, expiry_date) VALUES
('qT6uq2ZP1Yv2bWmt', 2, 1, NULL, 'invitee@konfigyr.com', 'ADMIN', CURRENT_TIMESTAMP + INTERVAL '7 DAYS'),
('B1LPctaRXp6sxRo7', 2, NULL, NULL, 'expiring@konfigyr.com', 'USER', CURRENT_TIMESTAMP - INTERVAL '10 DAYS');
