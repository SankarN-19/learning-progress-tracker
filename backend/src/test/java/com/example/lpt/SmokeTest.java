package com.example.lpt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** The application context starts and the basics are wired. */
class SmokeTest extends AbstractApiTest {

    @Test
    void contextStartsAndCoordinatorExists() {
        org.assertj.core.api.Assertions.assertThat(accounts.findByEmail(COORDINATOR_EMAIL)).isPresent();
    }

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.test\",\"password\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());
    }
}
