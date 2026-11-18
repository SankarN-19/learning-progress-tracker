package com.example.lpt.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "planned_hours", nullable = false)
    private Double plannedHours;

    protected Activity() {
    }

    public Activity(String title, String topic, Double plannedHours) {
        this.title = title;
        this.topic = topic;
        this.plannedHours = plannedHours;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getTopic() { return topic; }
    public Double getPlannedHours() { return plannedHours; }

    public void setTitle(String title) { this.title = title; }
    public void setTopic(String topic) { this.topic = topic; }
    public void setPlannedHours(Double plannedHours) { this.plannedHours = plannedHours; }
}
