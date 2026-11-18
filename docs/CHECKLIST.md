# Requirements checklist

Where each requirement of the capstone brief is implemented and which tests prove it.
Paths are relative to `backend/src/main/java/com/example/lpt/` (**BE**), `frontend/src/` (**FE**) and
`backend/src/test/java/com/example/lpt/` (**BT**); frontend tests are in `frontend/src/__tests__/` (**FT**).

## Functional requirements

| # | Requirement | Implemented in | Proven by |
| --- | --- | --- | --- |
| FR-1 | Show activities (title, topic, planned hours) | BE `controller/ActivityController` · `service/ActivityService.list` · FE `pages/ActivitiesPage`, `DashboardPage` | BT `ActivityApiTest.listIsEmptyWhenNothingSaved`, `savedActivitiesPersistAndAreListed` |
| FR-2 | Coordinator creates and edits an activity; all fields required; hours > 0 | BE `ActivityService.create/update`, `dto/ActivityRequest` (Bean Validation), `schema.sql` CHECK · FE `ActivityModal` | BT `ActivityApiTest.coordinatorCreatesActivity`, `createRejectsMissingFields`, `createRejectsHoursZeroOrNegative`, `createRejectsMissingOrNonNumericHours`, `coordinatorEditsActivity`, `editRejectsInvalidData`, `editUnknownActivityIsNotFound` · FT `ActivityModal.test.jsx` |
| FR-3 | Show learners and each learner's assigned activities | BE `LearnerController`, `AssignmentService.listForLearner` · FE `LearnersPage`, `LearnerDetailPage` | BT `AssignmentApiTest.learnerAssignmentsIncludeActivityDetails`, `LearnerApiTest.learnersListShowsProgressCounts` |
| FR-4 | Coordinator assigns an activity; duplicates not allowed | BE `AssignmentService.assign`, UNIQUE(learner_id, activity_id) in `schema.sql` and `entity/Assignment` · FE `LearnerDetailPage` (assign form, inline 409 message) | BT `AssignmentApiTest.assignmentStartsAsNotStarted`, `duplicateAssignmentIsRejected`, `assignRejectsUnknownLearnerOrActivity`, `assignRejectsMissingIds` · Mockito `service/AssignmentServiceTest.assignRejectsDuplicateWithoutSaving`, `assignTurnsDatabaseUniqueViolationIntoConflict` |
| FR-5 | Learner updates status: Not Started, In Progress, Completed | BE `AssignmentController.updateStatus`, `AssignmentService.updateStatus`, `entity/AssignmentStatus` · FE `MyTrainingPage` | BT `AssignmentApiTest.candidateUpdatesOwnStatus`, `invalidStatusIsRejected`, `unknownAssignmentIsNotFound` · FT `MyTrainingPage.test.jsx` |
| FR-6 | Cohort summary (learners, assignments, completed) calculated from saved data | BE `service/SummaryService` (live `count` queries, nothing cached) · FE `DashboardPage` | BT `CohortSummaryApiTest.emptyCohortHasZeroTotals`, `totalsMatchSavedAssignments`, `totalsFollowStatusChanges` |
| FR-7 | Coordinator adds a candidate (name, unique email, cohort) and provides initial credentials | BE `LearnerService.createCandidate`, `security/PasswordGenerator`, `LearnerController` (`Cache-Control: no-store`) · FE `AddCandidateModal`, `components/CredentialsBox` | BT `LearnerApiTest.addCandidateReturnsOneTimeCredentials`, `duplicateEmailIsRejectedIgnoringCase`, `emailOfExistingCoordinatorIsAlsoDuplicate`, `addCandidateRejectsMissingOrInvalidFields` · Mockito `LearnerServiceTest` · FT `AddCandidateModal.test.jsx` |
| FR-8 | Candidate login, first-login password change, logout; candidates see and update only their own training | BE `AuthController`, `AuthService`, `security/SessionManager`, `security/SessionGuardFilter`, `config/SecurityConfig`, `AssignmentService.updateStatus`/`listMine` · FE `AuthContext`, `auth/guards`, `LoginPage`, `ChangePasswordPage`, `MyTrainingPage` | BT `AuthApiTest` (all), `AssignmentApiTest.candidateCannotUpdateAnotherCandidatesAssignment`, `candidateSeesOnlyOwnAssignments`, `identityComesFromSessionNotFromRequest` · FT `LoginPage.test.jsx`, `ChangePasswordPage.test.jsx`, `guards.test.jsx` |

## Acceptance criteria (section 07 of the brief)

| Criterion | How to verify | Evidence |
| --- | --- | --- |
| Run the application using the repository setup instructions | Follow `README.md` (MySQL, `.env`, `mvn spring-boot:run`, `npm run dev`) | `README.md` |
| View seeded activities and learners, create or edit an activity, assign it, update its status | Demo sections 2, 3, 5, 7 | `data.sql`; `WorkflowTest` |
| Understandable feedback for invalid fields or a duplicate assignment | Demo sections 3 and 8b | inline messages in `ActivityModal`, `AddCandidateModal`, `LearnerDetailPage`; BT `ActivityApiTest`, `AssignmentApiTest.duplicateAssignmentIsRejected`; FT modal tests |
| Cohort summary matches saved assignments, including after a refresh | Demo section 7 (Refresh, then F5) | BT `CohortSummaryApiTest` |
| Empty state when there is no data; error state when saving fails | Fresh database shows empty states; stop the backend and save | `EmptyState` component on every list page; FT `MyTrainingPage.test.jsx` (empty and error), `ActivityModal.test.jsx` (save fails) |
| Automated tests for the core data rules and at least one main workflow | `mvn test` and `npm test` | BT `WorkflowTest.fullWorkflowFromCandidateCreationToCohortSummary` and the rule tests above |
| Demonstrate the workflow; explain the data model and the stack | `docs/DEMO.md`, sections 9 | `README.md` (Data model, Why this stack) |
| Create a candidate, privately hand over one-time credentials, reject duplicate email, complete first-login password change before viewing training | Demo sections 4 and 6 | BT `LearnerApiTest`, `AuthApiTest.firstLoginSessionCanOnlyChangePasswordAndLogout`, `changePasswordEndsOldSessionAndStartsFullAccess` |
| Reject a candidate listing all learners, creating accounts, or reading/updating another candidate's assignments by changing a URL or request body | Demo section 8a | BT `AccessControlApiTest.candidatesCannotUseCoordinatorEndpoints` (9 endpoints), `AssignmentApiTest.candidateCannotUpdateAnotherCandidatesAssignment`, `identityComesFromSessionNotFromRequest` |
| Reject logged-out sessions and expired temporary credentials; demonstrate coordinator reset | Demo sections 8c, 8d, 8e | BT `AccessControlApiTest.loggedOutCallersAreRejected`, `AuthApiTest.expiredTemporaryPasswordCannotLogIn`, `logoutEndsTheSession`, `sessionExpiredByServerIsRejected`, `LearnerApiTest.resetPasswordRestoresAccessAfterExpiry`, `resetPasswordEndsExistingSessions` |

## Data rules and security requirements

| Requirement | Where |
| --- | --- |
| Valid references; one assignment per learner/activity; hours > 0 | `schema.sql` (FKs, UNIQUE, CHECK) and services/DTO validation (both layers) |
| Fictional seed (the brief suggests 3 learners and 4 activities; the seed has those plus a second cohort) | `backend/src/main/resources/data.sql` |
| Spring Security, salted hashes (never plaintext) | `AppConfig` (`BCryptPasswordEncoder`); BT `LearnerApiTest.addCandidateReturnsOneTimeCredentials` checks the stored hash |
| Secure `HttpOnly` `SameSite` session cookie, session expiry | `application.properties` (`server.servlet.session.cookie.*`, timeout 30m); `SessionManager` (new session on login and on password change) |
| CSRF protection | `SecurityConfig` + `security/CsrfCookieFilter`; FE `api/client.js`; BT `AuthApiTest.requestsWithoutCsrfTokenAreRejected`, `SmokeTest` |
| Login rate limiting | `security/LoginRateLimiter`; BT `AuthApiTest.tooManyFailedLoginsAreRateLimited`, `security/LoginRateLimiterTest`, `AuthServiceTest` |
| Generic login error (no account enumeration) | `AuthService.login` (dummy hash, one message); BT `wrongPasswordAndUnknownEmailGiveTheSameGenericError` |
| Temporary password: random, expires, shown once | `PasswordGenerator`, `LearnerService`, `Cache-Control: no-store`; BT `PasswordGeneratorTest`, `LearnerApiTest` |
| Password-change-pending session limited to change-password and logout | `security/SessionGuardFilter`; BT `AuthApiTest.firstLoginSessionCanOnlyChangePasswordAndLogout` |
| Roles and ownership enforced on the server | `SecurityConfig` (roles), `AssignmentService` (ownership); BT `AccessControlApiTest`, `AssignmentApiTest` |
| No hash in any response; passwords never logged | DTOs; BT `AuthApiTest.responsesNeverExposePasswordHashes`; `application.properties` log levels |
| No committed secrets | `.gitignore` (`.env`), `backend/.env.example`, no credentials in `data.sql` (placeholder hashes only) |
| Initial coordinator from environment, no public registration | `config/CoordinatorBootstrap`; no register route or page exists |
| Mock authentication dependencies in unit tests | BT `service/AuthServiceTest`, `LearnerServiceTest`, `AssignmentServiceTest` (Mockito) |

## Deliverables

| Deliverable | Location |
| --- | --- |
| Application source | `backend/`, `frontend/` |
| Database initialisation and fictional seed data | `backend/src/main/resources/schema.sql`, `data.sql` |
| Automated tests with run instructions | `backend/src/test`, `frontend/src/__tests__`, `README.md` (Test) |
| README: prerequisites, configuration, setup, run, test | `README.md` |
| Short demo script | `docs/DEMO.md` |
| This checklist | `docs/CHECKLIST.md` |

## Known limits (stated honestly)

- **HTTPS** is required outside localhost by the brief. The app supports it (`Secure` cookie flag) but does not
  terminate TLS; use a reverse proxy.
- The login rate limiter is in memory (the brief allows this): it resets on restart and is per instance.
- The cohort summary updates when the page is opened, refreshed, or the tab regains focus; there is no live push.
- Invitation emails, CSV import/export, cloud deployment and monitoring are optional in the brief and not built.
- Small additions beyond the brief's API (all listed in `README.md`): `GET /auth/me`, status `429`,
  extra count fields on learners and the summary, and `DELETE` for activities and learners (added on request).
  Tests: `ActivityApiTest.deleteRemovesTheActivityAndItsAssignments`, `LearnerApiTest.deleteRemovesLearnerAccountAssignmentsAndSessions`,
  `AccessControlApiTest` (delete is coordinator-only), `DeleteFlows.test.jsx`.
