package com.example.lpt.service;

import com.example.lpt.dto.ActivityRequest;
import com.example.lpt.dto.ActivityResponse;
import com.example.lpt.entity.Activity;
import com.example.lpt.exception.ApiException;
import com.example.lpt.repository.ActivityRepository;
import com.example.lpt.repository.AssignmentRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

    private final ActivityRepository activities;
    private final AssignmentRepository assignments;

    public ActivityService(ActivityRepository activities, AssignmentRepository assignments) {
        this.activities = activities;
        this.assignments = assignments;
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> list() {
        return activities.findAll(Sort.by("id")).stream().map(ActivityResponse::from).toList();
    }

    @Transactional
    public ActivityResponse create(ActivityRequest request) {
        Activity saved = activities.save(
                new Activity(request.title().trim(), request.topic().trim(), request.plannedHours()));
        return ActivityResponse.from(saved);
    }

    @Transactional
    public ActivityResponse update(Long id, ActivityRequest request) {
        Activity activity = activities.findById(id)
                .orElseThrow(() -> ApiException.notFound("Activity not found."));
        activity.setTitle(request.title().trim());
        activity.setTopic(request.topic().trim());
        activity.setPlannedHours(request.plannedHours());
        return ActivityResponse.from(activity);
    }

    @Transactional
    public void delete(Long id) {
        Activity activity = activities.findById(id)
                .orElseThrow(() -> ApiException.notFound("Activity not found."));
        assignments.deleteByActivityId(id);
        activities.delete(activity);
    }
}
