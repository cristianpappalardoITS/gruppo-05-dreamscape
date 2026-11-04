-- V902__seed_test_users.sql
-- Utenti finti, solo nel profilo dev e nei test. Username e password sono nel README del
-- backend (sviluppo/backend/README.md); qui c'è solo la password cifrata con BCrypt.
-- Tutti partono come dopo la registrazione: 500 DC e fazione del set iniziale ancora da scegliere.
-- anna_test e bruno_test sono amici; carla_test ha chiesto l'amicizia a bruno_test.

INSERT INTO users (username, email, password_hash, credits, privacy_accepted_at, terms_accepted_at)
SELECT v.username, v.email, '$2a$10$0k5aD/7Ymchc45TYv8BWuenuvVwbew7442cCagi2j9kwZUhLggKvS', 500, now(), now()
FROM (VALUES
    ('anna_test',  'anna_test@example.test'),
    ('bruno_test', 'bruno_test@example.test'),
    ('carla_test', 'carla_test@example.test')
) AS v (username, email);

-- Il bonus di registrazione, come lo scrive il service.
INSERT INTO credit_transactions (user_id, type, amount, balance_after)
SELECT id, 'SIGNUP_BONUS', 500, 500
FROM users
WHERE username IN ('anna_test', 'bruno_test', 'carla_test');

INSERT INTO friendships (requester_id, addressee_id, status, accepted_at)
SELECT a.id, b.id, 'ACCEPTED', now()
FROM users a, users b
WHERE a.username = 'anna_test' AND b.username = 'bruno_test';

INSERT INTO friendships (requester_id, addressee_id, status)
SELECT c.id, b.id, 'PENDING'
FROM users c, users b
WHERE c.username = 'carla_test' AND b.username = 'bruno_test';
