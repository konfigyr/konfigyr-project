INSERT INTO accounts(id, status, email, first_name, last_name, last_login_at) VALUES
(1, 'ACTIVE', 'john.doe@konfigyr.com', 'John', 'Doe', now() - interval '5 minutes'),
(2, 'INITIAL', 'jane.doe@konfigyr.com', 'Jane', 'Doe', NULL);
