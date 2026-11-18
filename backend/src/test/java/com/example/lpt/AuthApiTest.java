package com.example.lpt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/** FR-8: login, first-login enforcement, password change, logout, session expiry, rate limiting. */
class AuthApiTest extends AbstractApiTest {

    @Test
    void coordinatorLoginReturnsRoleAndPasswordFlag() throws Exception {
        send(HttpMethod.POST, LOGIN, null, creds(COORDINATOR_EMAIL, COORDINATOR_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COORDINATOR"))
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void loginEmailIsCaseInsensitive() throws Exception {
        send(HttpMethod.POST, LOGIN, null, creds("  Coordinator@EXAMPLE.test ", COORDINATOR_PASSWORD))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameGenericError() throws Exception {
        String unknownEmail = send(HttpMethod.POST, LOGIN, null, creds("nobody@example.test", "whatever-123"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String wrongPassword = send(HttpMethod.POST, LOGIN, null, creds(COORDINATOR_EMAIL, "wrong-password"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        assertThat(unknownEmail).isEqualTo(wrongPassword);
        assertThat(unknownEmail).contains("Invalid email or password.");
    }

    @Test
    void loginWithMissingFieldsIsBadRequest() throws Exception {
        send(HttpMethod.POST, LOGIN, null, Map.of("email", "a@b.test")).andExpect(status().isBadRequest());
        send(HttpMethod.POST, LOGIN, null, Map.of("password", "x")).andExpect(status().isBadRequest());
        send(HttpMethod.POST, LOGIN, null, "{}").andExpect(status().isBadRequest());
    }

    @Test
    void expiredTemporaryPasswordCannotLogIn() throws Exception {
        Created created = createCandidate(coordinator(), "Exp Ired", "expired@example.test", "Cohort");
        expireTemporaryPassword(created.email());

        send(HttpMethod.POST, LOGIN, null, creds(created.email(), created.temporaryPassword()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void temporaryPasswordThatExpiresMidSessionCannotChangePassword() throws Exception {
        Created created = createCandidate(coordinator(), "Late Comer", "late@example.test", "Cohort");
        MockHttpSession session = login(created.email(), created.temporaryPassword());
        expireTemporaryPassword(created.email());

        send(HttpMethod.POST, "/api/v1/auth/change-password", session,
                Map.of("currentPassword", created.temporaryPassword(), "newPassword", NEW_PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("expired")));
        getAs("/api/v1/auth/me", session).andExpect(status().isUnauthorized());
    }

    @Test
    void firstLoginSessionCanOnlyChangePasswordAndLogout() throws Exception {
        Created created = createCandidate(coordinator(), "New Hire", "hire@example.test", "Cohort");

        MvcResult login = send(HttpMethod.POST, LOGIN, null, creds(created.email(), created.temporaryPassword()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        // training data is blocked until the password is changed
        getAs("/api/v1/me/assignments", session).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("temporary password")));
        send(HttpMethod.PATCH, "/api/v1/assignments/1/status", session, Map.of("status", "Completed"))
                .andExpect(status().isForbidden());
        // identity check is allowed (the UI uses it after a refresh) and reports the pending change
        getAs("/api/v1/auth/me", session).andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
        // logout is always allowed
        send(HttpMethod.POST, "/api/v1/auth/logout", session, null).andExpect(status().isNoContent());
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() throws Exception {
        Created created = createCandidate(coordinator(), "Pat Wrong", "pat.wrong@example.test", "Cohort");
        MockHttpSession session = login(created.email(), created.temporaryPassword());

        send(HttpMethod.POST, "/api/v1/auth/change-password", session,
                Map.of("currentPassword", "not-the-password", "newPassword", NEW_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currentPassword").exists());
    }

    @Test
    void changePasswordRejectsShortNewPassword() throws Exception {
        Created created = createCandidate(coordinator(), "Pat Short", "pat.short@example.test", "Cohort");
        MockHttpSession session = login(created.email(), created.temporaryPassword());

        send(HttpMethod.POST, "/api/v1/auth/change-password", session,
                Map.of("currentPassword", created.temporaryPassword(), "newPassword", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());
    }

    @Test
    void changePasswordRejectsReusingTheTemporaryPassword() throws Exception {
        Created created = createCandidate(coordinator(), "Pat Same", "pat.same@example.test", "Cohort");
        MockHttpSession session = login(created.email(), created.temporaryPassword());

        send(HttpMethod.POST, "/api/v1/auth/change-password", session,
                Map.of("currentPassword", created.temporaryPassword(), "newPassword", created.temporaryPassword()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());
    }

    @Test
    void changePasswordEndsOldSessionAndStartsFullAccess() throws Exception {
        Created created = createCandidate(coordinator(), "Pat Lee", "pat@example.test", "Cohort");
        MockHttpSession first = login(created.email(), created.temporaryPassword());

        MvcResult changed = send(HttpMethod.POST, "/api/v1/auth/change-password", first,
                Map.of("currentPassword", created.temporaryPassword(), "newPassword", NEW_PASSWORD))
                .andExpect(status().isNoContent()).andReturn();
        MockHttpSession fresh = (MockHttpSession) changed.getRequest().getSession(false);

        getAs("/api/v1/auth/me", first).andExpect(status().isUnauthorized());          // old session is gone
        getAs("/api/v1/me/assignments", fresh).andExpect(status().isOk())              // new session has full access
                .andExpect(content().json("[]"));
        send(HttpMethod.POST, LOGIN, null, creds(created.email(), created.temporaryPassword()))
                .andExpect(status().isUnauthorized());                                  // temp password is dead
        send(HttpMethod.POST, LOGIN, null, creds(created.email(), NEW_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        MockHttpSession session = coordinator();
        getAs("/api/v1/activities", session).andExpect(status().isOk());

        send(HttpMethod.POST, "/api/v1/auth/logout", session, null).andExpect(status().isNoContent());

        getAs("/api/v1/activities", session).andExpect(status().isUnauthorized());
    }

    @Test
    void sessionExpiredByServerIsRejected() throws Exception {
        MockHttpSession session = coordinator();
        sessionRegistry.getSessionInformation(session.getId()).expireNow();

        getAs("/api/v1/activities", session)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Your session has ended. Please sign in again."));
    }

    @Test
    void sessionExpiredByContainerIsRejected() throws Exception {
        MockHttpSession session = coordinator();
        session.invalidate(); // what the servlet container does when the session timeout passes

        getAs("/api/v1/activities", session)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required."));
    }

    @Test
    void tooManyFailedLoginsAreRateLimited() throws Exception {
        for (int i = 0; i < 5; i++) {
            send(HttpMethod.POST, LOGIN, null, creds(COORDINATOR_EMAIL, "wrong-password-" + i))
                    .andExpect(status().isUnauthorized());
        }
        // even the correct password is refused while the limit is active
        send(HttpMethod.POST, LOGIN, null, creds(COORDINATOR_EMAIL, COORDINATOR_PASSWORD))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many sign-in attempts. Please try again later."));
    }

    @Test
    void requestsWithoutCsrfTokenAreRejected() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(creds(COORDINATOR_EMAIL, COORDINATOR_PASSWORD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void responsesNeverExposePasswordHashes() throws Exception {
        MockHttpSession coordinator = coordinator();
        StringBuilder everything = new StringBuilder();
        everything.append(send(HttpMethod.POST, "/api/v1/learners", coordinator,
                Map.of("name", "Hash Check", "email", "hash@example.test", "cohort", "C"))
                .andReturn().getResponse().getContentAsString());
        everything.append(getAs("/api/v1/learners", coordinator).andReturn().getResponse().getContentAsString());
        everything.append(getAs("/api/v1/auth/me", coordinator).andReturn().getResponse().getContentAsString());

        assertThat(everything.toString().toLowerCase())
                .doesNotContain("passwordhash", "password_hash", "$2a$", "$2b$");
    }
}
