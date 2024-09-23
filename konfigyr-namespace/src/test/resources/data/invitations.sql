INSERT INTO invitations(key, namespace_id, sender_id, email, role, expiry_date) VALUES
('qT6uq2ZP1Yv2bWmt', 2, 1, 'invitee@konfigyr.com', 'ADMIN', now() + INTERVAL '7 DAYS'),
('B1LPctaRXp6sxRo7', 2, NULL, 'expiring@konfigyr.com', 'USER', now() - INTERVAL '10 DAYS');
