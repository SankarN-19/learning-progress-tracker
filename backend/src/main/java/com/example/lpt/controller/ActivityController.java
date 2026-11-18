package com.example.lpt.controller;

import com.example.lpt.dto.ActivityRequest;
import com.example.lpt.dto.ActivityResponse;
import com.example.lpt.service.ActivityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActivityResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityResponse create(@Valid @RequestBody ActivityRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ActivityResponse update(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
