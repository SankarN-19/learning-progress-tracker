package com.example.lpt.controller;

import com.example.lpt.dto.CohortSummaryResponse;
import com.example.lpt.service.SummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cohort-summary")
public class CohortSummaryController {

    private final SummaryService service;

    public CohortSummaryController(SummaryService service) {
        this.service = service;
    }

    @GetMapping
    public CohortSummaryResponse summary() {
        return service.summary();
    }
}
