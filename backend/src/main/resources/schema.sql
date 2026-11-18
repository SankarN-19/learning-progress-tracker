-- Learning Progress Tracker schema (MySQL 8). Safe to run on every start.

CREATE TABLE IF NOT EXISTS accounts (
  id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
  email                    VARCHAR(254) NOT NULL,            -- stored lower-cased
  password_hash            VARCHAR(100) NOT NULL,            -- BCrypt, never returned by the API
  role                     VARCHAR(20)  NOT NULL,            -- COORDINATOR | CANDIDATE
  must_change_password     BOOLEAN      NOT NULL DEFAULT FALSE,
  temp_password_expires_at DATETIME     NULL,
  CONSTRAINT uq_accounts_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS learners (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  display_name VARCHAR(100) NOT NULL,
  cohort       VARCHAR(100) NOT NULL,
  account_id   BIGINT       NOT NULL,
  CONSTRAINT uq_learners_account UNIQUE (account_id),
  CONSTRAINT fk_learners_account FOREIGN KEY (account_id) REFERENCES accounts (id)
);

CREATE TABLE IF NOT EXISTS activities (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  title         VARCHAR(150) NOT NULL,
  topic         VARCHAR(100) NOT NULL,
  planned_hours DOUBLE       NOT NULL,
  CONSTRAINT chk_activities_hours CHECK (planned_hours > 0)
);

CREATE TABLE IF NOT EXISTS assignments (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  learner_id  BIGINT      NOT NULL,
  activity_id BIGINT      NOT NULL,
  status      VARCHAR(20) NOT NULL DEFAULT 'Not Started',   -- Not Started | In Progress | Completed
  CONSTRAINT uq_assignments_learner_activity UNIQUE (learner_id, activity_id),
  CONSTRAINT fk_assignments_learner  FOREIGN KEY (learner_id)  REFERENCES learners (id),
  CONSTRAINT fk_assignments_activity FOREIGN KEY (activity_id) REFERENCES activities (id)
);
