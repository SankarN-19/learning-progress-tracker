package com.example.lpt.controller;

import com.example.lpt.dto.AssignmentResponse;
import com.example.lpt.security.AccountPrincipal;
import com.example.lpt.service.AssignmentService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final AssignmentService service;

    public MeController(AssignmentService service) {
        this.service = service;
    }

    @GetMapping("/assignments")
    public List<AssignmentResponse> myAssignments(@AuthenticationPrincipal AccountPrincipal principal) {
        return service.listMine(principal.accountId());
    }
}
