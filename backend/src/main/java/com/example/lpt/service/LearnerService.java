package com.example.lpt.service;

import com.example.lpt.dto.CandidateCredentialsResponse;
import com.example.lpt.dto.CreateLearnerRequest;
import com.example.lpt.dto.CredentialsResponse;
import com.example.lpt.dto.LearnerResponse;
import com.example.lpt.entity.Account;
import com.example.lpt.entity.AssignmentStatus;
import com.example.lpt.entity.Learner;
import com.example.lpt.entity.Role;
import com.example.lpt.exception.ApiException;
import com.example.lpt.repository.AccountRepository;
import com.example.lpt.repository.AssignmentRepository;
import com.example.lpt.repository.LearnerAssignmentCount;
import com.example.lpt.repository.LearnerRepository;
import com.example.lpt.security.PasswordGenerator;
import com.example.lpt.security.SessionManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearnerService {

    private static final String DUPLICATE_EMAIL = "A candidate with this email already exists.";

    private final LearnerRepository learners;
    private final AccountRepository accounts;
    private final AssignmentRepository assignments;
    private final PasswordEncoder encoder;
    private final PasswordGenerator generator;
    private final SessionManager sessions;
    private final Clock clock;
    private final Duration tempPasswordLifetime;

    public LearnerService(LearnerRepository learners, AccountRepository accounts, AssignmentRepository assignments,
                          PasswordEncoder encoder, PasswordGenerator generator, SessionManager sessions,
                          Clock clock, @Value("${app.temp-password.hours:24}") long tempPasswordHours) {
        this.learners = learners;
        this.accounts = accounts;
        this.assignments = assignments;
        this.encoder = encoder;
        this.generator = generator;
        this.sessions = sessions;
        this.clock = clock;
        this.tempPasswordLifetime = Duration.ofHours(tempPasswordHours);
    }

    @Transactional(readOnly = true)
    public List<LearnerResponse> list() {
        Map<Long, Long> totals = toMap(assignments.countPerLearner());
        Map<Long, Long> completed = toMap(assignments.countPerLearnerWithStatus(AssignmentStatus.COMPLETED));
        return learners.findAllWithAccount().stream()
                .map(l -> LearnerResponse.from(l, totals.getOrDefault(l.getId(), 0L),
                        completed.getOrDefault(l.getId(), 0L)))
                .toList();
    }

    @Transactional
    public CandidateCredentialsResponse createCandidate(CreateLearnerRequest request) {
        String email = AuthService.normalize(request.email());
        if (accounts.existsByEmail(email)) {
            throw ApiException.conflict("email", DUPLICATE_EMAIL);
        }

        String temporaryPassword = generator.generate();
        Instant expiresAt = clock.instant().plus(tempPasswordLifetime);
        Account account = new Account(email, encoder.encode(temporaryPassword), Role.CANDIDATE, true, expiresAt);

        Learner learner;
        try {
            accounts.saveAndFlush(account);
            learner = learners.saveAndFlush(new Learner(request.name().trim(), request.cohort().trim(), account));
        } catch (DataIntegrityViolationException e) { // two coordinators adding the same email at once
            throw ApiException.conflict("email", DUPLICATE_EMAIL);
        }

        return new CandidateCredentialsResponse(
                LearnerResponse.from(learner, 0, 0),
                new CredentialsResponse(email, temporaryPassword, expiresAt));
    }

    @Transactional
    public CandidateCredentialsResponse resetPassword(Long learnerId) {
        Learner learner = learners.findById(learnerId)
                .orElseThrow(() -> ApiException.notFound("Learner not found."));
        Account account = learner.getAccount();

        String temporaryPassword = generator.generate();
        Instant expiresAt = clock.instant().plus(tempPasswordLifetime);
        account.setPasswordHash(encoder.encode(temporaryPassword));
        account.setMustChangePassword(true);
        account.setTempPasswordExpiresAt(expiresAt);
        accounts.save(account);

        sessions.expireAllFor(account.getId()); // prior sessions stop working immediately

        long assigned = assignments.countByLearnerId(learnerId);
        long completed = assignments.countByLearnerIdAndStatus(learnerId, AssignmentStatus.COMPLETED);
        return new CandidateCredentialsResponse(
                LearnerResponse.from(learner, assigned, completed),
                new CredentialsResponse(account.getEmail(), temporaryPassword, expiresAt));
    }

    /**
     * Removes the learner, their assignments and their account, and ends their sessions.
     * Their email becomes available again.
     */
    @Transactional
    public void delete(Long learnerId) {
        Learner learner = learners.findById(learnerId)
                .orElseThrow(() -> ApiException.notFound("Learner not found."));
        Account account = learner.getAccount();
        Long accountId = account.getId();

        assignments.deleteByLearnerId(learnerId);
        learners.delete(learner);
        accounts.delete(account);
        sessions.expireAllFor(accountId);
    }

    private static Map<Long, Long> toMap(List<LearnerAssignmentCount> rows) {
        Map<Long, Long> map = new HashMap<>();
        rows.forEach(r -> map.put(r.getLearnerId(), r.getTotal()));
        return map;
    }
}
