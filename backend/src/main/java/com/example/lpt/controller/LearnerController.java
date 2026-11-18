package com.example.lpt.controller;

import com.example.lpt.dto.AssignmentResponse;
import com.example.lpt.dto.CandidateCredentialsResponse;
import com.example.lpt.dto.CreateLearnerRequest;
import com.example.lpt.dto.LearnerResponse;
import com.example.lpt.service.AssignmentService;
import com.example.lpt.service.LearnerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/learners")
public class LearnerController {

    private final LearnerService learnerService;
    private final AssignmentService assignmentService;

    public LearnerController(LearnerService learnerService, AssignmentService assignmentService) {
        this.learnerService = learnerService;
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<LearnerResponse> list() {
        return learnerService.list();
    }

    @GetMapping("/{id}/assignments")
    public List<AssignmentResponse> assignments(@PathVariable Long id) {
        return assignmentService.listForLearner(id);
    }

    /** Returns the candidate plus one-time credentials; must never be cached. */
    @PostMapping
    public ResponseEntity<CandidateCredentialsResponse> create(@Valid @RequestBody CreateLearnerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(learnerService.createCandidate(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        learnerService.delete(id);
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<CandidateCredentialsResponse> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(learnerService.resetPassword(id));
    }
}
