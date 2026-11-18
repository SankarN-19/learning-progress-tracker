-- Fictional demo data only. Every statement is idempotent, so it is safe to run on every start:
-- unique keys plus INSERT IGNORE, or a NOT EXISTS check, mean nothing is ever duplicated.
--
-- Loads 8 learners in two cohorts, 10 activities and 20 assignments with mixed statuses.
--
-- Learner accounts get an UNUSABLE placeholder hash ('!' is not a valid BCrypt hash,
-- so no password can ever match it). Nobody can sign in as them until the coordinator
-- uses "Reset password" to issue one-time temporary credentials.
-- The coordinator account is NOT seeded here. It is created from environment
-- variables on first run (see CoordinatorBootstrap).

-- ============================================================================
-- Part 1: base data (learners 1-3, activities 1-4, assignments 1-6)
-- ============================================================================

INSERT IGNORE INTO accounts (email, password_hash, role, must_change_password, temp_password_expires_at) VALUES
  ('jordan.lee@example.test',   '!', 'CANDIDATE', TRUE, NULL),
  ('priya.nair@example.test',   '!', 'CANDIDATE', TRUE, NULL),
  ('sam.okafor@example.test',   '!', 'CANDIDATE', TRUE, NULL);

INSERT IGNORE INTO learners (id, display_name, cohort, account_id) VALUES
  (1, 'Jordan Lee',  'Frontend cohort', (SELECT id FROM accounts WHERE email = 'jordan.lee@example.test')),
  (2, 'Priya Nair',  'Frontend cohort', (SELECT id FROM accounts WHERE email = 'priya.nair@example.test')),
  (3, 'Sam Okafor',  'Frontend cohort', (SELECT id FROM accounts WHERE email = 'sam.okafor@example.test'));

INSERT IGNORE INTO activities (id, title, topic, planned_hours) VALUES
  (1, 'React Basics',          'Frontend', 6),
  (2, 'REST APIs',             'Backend',  8),
  (3, 'SQL Foundations',       'Data',     4),
  (4, 'Git and Collaboration', 'Tooling',  3);

INSERT IGNORE INTO assignments (id, learner_id, activity_id, status) VALUES
  (1, 1, 1, 'In Progress'),
  (2, 1, 2, 'Not Started'),
  (3, 1, 3, 'Completed'),
  (4, 2, 1, 'Completed'),
  (5, 2, 2, 'In Progress'),
  (6, 3, 4, 'Not Started');

-- ============================================================================
-- Part 2: more data (a Backend cohort, 5 more learners, 6 more activities, 14 more assignments)
-- Matched by email or title instead of fixed ids, so it never clashes with records
-- the coordinator has created in the app.
-- ============================================================================

-- ---- Activities (matched by title) -----------------------------------------
INSERT INTO activities (title, topic, planned_hours)
SELECT t.title, t.topic, t.hours
FROM (
  SELECT 'Spring Boot Fundamentals' AS title, 'Backend'  AS topic, 10 AS hours
  UNION ALL SELECT 'Spring Security Essentials',   'Backend',   8
  UNION ALL SELECT 'JPA and Relational Modelling', 'Data',      6
  UNION ALL SELECT 'React Router and State',       'Frontend',  7
  UNION ALL SELECT 'Testing with JUnit and Mockito', 'Quality', 5
  UNION ALL SELECT 'Accessible Interfaces',        'Frontend',  4
) AS t
WHERE NOT EXISTS (SELECT 1 FROM activities a WHERE a.title = t.title);

-- ---- Accounts for the new learners (email is unique, so IGNORE is safe) ----
INSERT IGNORE INTO accounts (email, password_hash, role, must_change_password, temp_password_expires_at) VALUES
  ('aisha.khan@example.test',   '!', 'CANDIDATE', TRUE, NULL),
  ('mateo.silva@example.test',  '!', 'CANDIDATE', TRUE, NULL),
  ('hana.sato@example.test',    '!', 'CANDIDATE', TRUE, NULL),
  ('liam.murphy@example.test',  '!', 'CANDIDATE', TRUE, NULL),
  ('zara.ahmed@example.test',   '!', 'CANDIDATE', TRUE, NULL);

-- ---- Learners linked to those accounts -------------------------------------
INSERT INTO learners (display_name, cohort, account_id)
SELECT t.name, t.cohort, a.id
FROM (
  SELECT 'Aisha Khan'   AS name, 'Backend cohort'  AS cohort, 'aisha.khan@example.test'  AS email
  UNION ALL SELECT 'Mateo Silva', 'Backend cohort',  'mateo.silva@example.test'
  UNION ALL SELECT 'Hana Sato',   'Backend cohort',  'hana.sato@example.test'
  UNION ALL SELECT 'Liam Murphy', 'Frontend cohort', 'liam.murphy@example.test'
  UNION ALL SELECT 'Zara Ahmed',  'Frontend cohort', 'zara.ahmed@example.test'
) AS t
JOIN accounts a ON a.email = t.email
WHERE NOT EXISTS (SELECT 1 FROM learners l WHERE l.account_id = a.id);

-- ---- Assignments (UNIQUE(learner, activity) makes re-runs harmless) --------
INSERT IGNORE INTO assignments (learner_id, activity_id, status)
SELECT l.id, act.id, t.status
FROM (
  SELECT 'aisha.khan@example.test'  AS email, 'Spring Boot Fundamentals'    AS title, 'Completed'   AS status
  UNION ALL SELECT 'aisha.khan@example.test',  'Spring Security Essentials',     'In Progress'
  UNION ALL SELECT 'aisha.khan@example.test',  'REST APIs',                      'Completed'
  UNION ALL SELECT 'mateo.silva@example.test', 'Spring Boot Fundamentals',       'In Progress'
  UNION ALL SELECT 'mateo.silva@example.test', 'JPA and Relational Modelling',   'Not Started'
  UNION ALL SELECT 'mateo.silva@example.test', 'SQL Foundations',                'Completed'
  UNION ALL SELECT 'hana.sato@example.test',   'Spring Boot Fundamentals',       'Not Started'
  UNION ALL SELECT 'hana.sato@example.test',   'Testing with JUnit and Mockito', 'Not Started'
  UNION ALL SELECT 'liam.murphy@example.test', 'React Basics',                   'Completed'
  UNION ALL SELECT 'liam.murphy@example.test', 'React Router and State',         'In Progress'
  UNION ALL SELECT 'liam.murphy@example.test', 'Accessible Interfaces',          'Not Started'
  UNION ALL SELECT 'zara.ahmed@example.test',  'React Basics',                   'In Progress'
  UNION ALL SELECT 'zara.ahmed@example.test',  'Accessible Interfaces',          'Completed'
  UNION ALL SELECT 'zara.ahmed@example.test',  'Git and Collaboration',          'Completed'
) AS t
JOIN accounts   a   ON a.email = t.email
JOIN learners   l   ON l.account_id = a.id
JOIN activities act ON act.title = t.title;