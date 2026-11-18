package com.example.lpt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.lpt.dto.CandidateCredentialsResponse;
import com.example.lpt.dto.CreateLearnerRequest;
import com.example.lpt.entity.Account;
import com.example.lpt.entity.Learner;
import com.example.lpt.entity.Role;
import com.example.lpt.exception.ApiException;
import com.example.lpt.repository.AccountRepository;
import com.example.lpt.repository.AssignmentRepository;
import com.example.lpt.repository.LearnerRepository;
import com.example.lpt.security.PasswordGenerator;
import com.example.lpt.security.SessionManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LearnerServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-01T09:00:00Z");

    @Mock LearnerRepository learners;
    @Mock AccountRepository accounts;
    @Mock AssignmentRepository assignments;
    @Mock PasswordEncoder encoder;
    @Mock PasswordGenerator generator;
    @Mock SessionManager sessions;

    LearnerService service;

    @BeforeEach
    void setUp() {
        service = new LearnerService(learners, accounts, assignments, encoder, generator, sessions,
                Clock.fixed(NOW, ZoneOffset.UTC), 24);
    }

    @Test
    void duplicateEmailIsRejectedAfterNormalisingTheEmail() {
        when(accounts.existsByEmail("jane@example.test")).thenReturn(true);

        assertThatThrownBy(() -> service.createCandidate(
                new CreateLearnerRequest("Jane", "  Jane@Example.TEST ", "Cohort")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getField()).isEqualTo("email");
                });
        verifyNoInteractions(generator);
        verify(accounts, never()).saveAndFlush(any());
    }

    @Test
    void concurrentDuplicateIsStillAConflict() {
        when(accounts.existsByEmail("jane@example.test")).thenReturn(false);
        when(generator.generate()).thenReturn("TempPass12345X");
        when(encoder.encode("TempPass12345X")).thenReturn("hash");
        when(accounts.saveAndFlush(any(Account.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.createCandidate(new CreateLearnerRequest("Jane", "jane@example.test", "C")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createCandidateStoresOnlyAHashAndReturnsTheTemporaryPasswordOnce() {
        when(accounts.existsByEmail("jane@example.test")).thenReturn(false);
        when(generator.generate()).thenReturn("TempPass12345X");
        when(encoder.encode("TempPass12345X")).thenReturn("$2a$10$hash");
        when(accounts.saveAndFlush(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(learners.saveAndFlush(any(Learner.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidateCredentialsResponse result =
                service.createCandidate(new CreateLearnerRequest("  Jane Doe ", "Jane@Example.TEST", " Backend "));

        ArgumentCaptor<Account> account = ArgumentCaptor.forClass(Account.class);
        verify(accounts).saveAndFlush(account.capture());
        assertThat(account.getValue().getEmail()).isEqualTo("jane@example.test");
        assertThat(account.getValue().getRole()).isEqualTo(Role.CANDIDATE);
        assertThat(account.getValue().isMustChangePassword()).isTrue();
        assertThat(account.getValue().getPasswordHash()).isEqualTo("$2a$10$hash").isNotEqualTo("TempPass12345X");
        assertThat(account.getValue().getTempPasswordExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));

        assertThat(result.credentials().username()).isEqualTo("jane@example.test");
        assertThat(result.credentials().temporaryPassword()).isEqualTo("TempPass12345X");
        assertThat(result.credentials().expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThat(result.learner().displayName()).isEqualTo("Jane Doe");
        assertThat(result.learner().cohort()).isEqualTo("Backend");
    }

    @Test
    void resetPasswordIssuesNewCredentialsAndEndsSessions() {
        Account account = new Account("jane@example.test", "old-hash", Role.CANDIDATE, false, null);
        ReflectionTestUtils.setField(account, "id", 5L);
        Learner learner = new Learner("Jane Doe", "Cohort", account);
        ReflectionTestUtils.setField(learner, "id", 3L);
        when(learners.findById(3L)).thenReturn(Optional.of(learner));
        when(generator.generate()).thenReturn("NewTemp1234567");
        when(encoder.encode("NewTemp1234567")).thenReturn("new-hash");

        CandidateCredentialsResponse result = service.resetPassword(3L);

        assertThat(account.getPasswordHash()).isEqualTo("new-hash");
        assertThat(account.isMustChangePassword()).isTrue();
        assertThat(account.getTempPasswordExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        verify(accounts).save(account);
        verify(sessions).expireAllFor(5L);
        assertThat(result.credentials().temporaryPassword()).isEqualTo("NewTemp1234567");
    }

    @Test
    void deleteRemovesAssignmentsThenLearnerThenAccountAndEndsSessions() {
        Account account = new Account("jane@example.test", "hash", Role.CANDIDATE, false, null);
        ReflectionTestUtils.setField(account, "id", 5L);
        Learner learner = new Learner("Jane Doe", "Cohort", account);
        ReflectionTestUtils.setField(learner, "id", 3L);
        when(learners.findById(3L)).thenReturn(Optional.of(learner));

        service.delete(3L);

        InOrder order = inOrder(assignments, learners, accounts);
        order.verify(assignments).deleteByLearnerId(3L);
        order.verify(learners).delete(learner);
        order.verify(accounts).delete(account);
        verify(sessions).expireAllFor(5L);
    }

    @Test
    void deleteUnknownLearnerIsNotFound() {
        when(learners.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(3L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(assignments, accounts, sessions);
    }

    @Test
    void resetPasswordForUnknownLearnerIsNotFound() {
        when(learners.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(3L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(sessions, generator);
    }
}
