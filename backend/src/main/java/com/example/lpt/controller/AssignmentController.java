package com.example.lpt.controller;

import com.example.lpt.dto.AssignmentRequest;
import com.example.lpt.dto.AssignmentResponse;
import com.example.lpt.dto.StatusUpdateRequest;
import com.example.lpt.security.AccountPrincipal;
import com.example.lpt.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentController {

    private final AssignmentService service;

    public AssignmentController(AssignmentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentResponse assign(@Valid @RequestBody AssignmentRequest request) {
        return service.assign(request);
    }

    @PatchMapping("/{id}/status")
    public AssignmentResponse updateStatus(@PathVariable Long id,
                                           @Valid @RequestBody StatusUpdateRequest request,
                                           @AuthenticationPrincipal AccountPrincipal principal) {
        return service.updateStatus(id, principal.accountId(), request.status());
    }
}
