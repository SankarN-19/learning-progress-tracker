package com.example.lpt.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "assignments",
        uniqueConstraints = @UniqueConstraint(name = "uq_assignments_learner_activity",
                columnNames = {"learner_id", "activity_id"}))
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    private Learner learner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Convert(converter = AssignmentStatusConverter.class)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.NOT_STARTED;

    protected Assignment() {
    }

    public Assignment(Learner learner, Activity activity) {
        this.learner = learner;
        this.activity = activity;
        this.status = AssignmentStatus.NOT_STARTED;
    }

    public Long getId() { return id; }
    public Learner getLearner() { return learner; }
    public Activity getActivity() { return activity; }
    public AssignmentStatus getStatus() { return status; }

    public void setStatus(AssignmentStatus status) { this.status = status; }
}
