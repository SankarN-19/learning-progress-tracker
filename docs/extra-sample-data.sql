-- ============================================================================
-- Extra FICTIONAL sample data for Learning Progress Tracker (MySQL 8)
-- Run in MySQL Workbench AFTER the app has started at least once
-- (the app creates the tables). Safe to run more than once: nothing is duplicated.
--
-- Adds: 6 activities, 5 learners (a second cohort), 14 assignments.
-- Learner accounts get an UNUSABLE placeholder password ('!'), so nobody can sign
-- in as them until the coordinator clicks "Reset password" on the learner's page
-- and hands over the one-time credentials.
-- ============================================================================
USE learning_tracker;

-- ---- 1. Activities (matched by title, so re-running adds nothing) ----------
INSERT INTO activities (title, topic, planned_hours)
SELECT t.title, t.topic, t.hours
FROM (
  SELECT 'Spring Boot Fundamentals' AS title, 'Backend'  AS topic, 10 AS hours
  UNION ALL SELECT 'Spring Security Essentials', 'Backend',   8
  UNION ALL SELECT 'JPA and Relational Modelling', 'Data',    6
  UNION ALL SELECT 'React Router and State',       'Frontend', 7
  UNION ALL SELECT 'Testing with JUnit and Mockito', 'Quality', 5
  UNION ALL SELECT 'Accessible Interfaces',        'Frontend', 4
) AS t
WHERE NOT EXISTS (SELECT 1 FROM activities a WHERE a.title = t.title);

-- ---- 2. Accounts for the new learners (email is unique, so IGNORE is safe) --
INSERT IGNORE INTO accounts (email, password_hash, role, must_change_password, temp_password_expires_at) VALUES
  ('aisha.khan@example.test',   '!', 'CANDIDATE', TRUE, NULL),
  ('mateo.silva@example.test',  '!', 'CANDIDATE', TRUE, NULL),
  ('hana.sato@example.test',    '!', 'CANDIDATE', TRUE, NULL),
  ('liam.murphy@example.test',  '!', 'CANDIDATE', TRUE, NULL),
  ('zara.ahmed@example.test',   '!', 'CANDIDATE', TRUE, NULL);

-- ---- 3. Learners linked to those accounts ----------------------------------
INSERT INTO learners (display_name, cohort, account_id)
SELECT t.name, t.cohort, a.id
FROM (
  SELECT 'Aisha Khan'   AS name, 'Backend cohort' AS cohort, 'aisha.khan@example.test'  AS email
  UNION ALL SELECT 'Mateo Silva',  'Backend cohort', 'mateo.silva@example.test'
  UNION ALL SELECT 'Hana Sato',    'Backend cohort', 'hana.sato@example.test'
  UNION ALL SELECT 'Liam Murphy',  'Frontend cohort', 'liam.murphy@example.test'
  UNION ALL SELECT 'Zara Ahmed',   'Frontend cohort', 'zara.ahmed@example.test'
) AS t
JOIN accounts a ON a.email = t.email
WHERE NOT EXISTS (SELECT 1 FROM learners l WHERE l.account_id = a.id);

-- ---- 4. Assignments (UNIQUE(learner, activity) makes re-runs harmless) -----
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

-- ---- 5. Check the result ----------------------------------------------------
SELECT l.display_name, l.cohort, a.email,
       COUNT(s.id) AS assigned,
       SUM(s.status = 'Completed') AS completed
FROM learners l
JOIN accounts a ON a.id = l.account_id
LEFT JOIN assignments s ON s.learner_id = l.id
GROUP BY l.id, l.display_name, l.cohort, a.email
ORDER BY l.id;
