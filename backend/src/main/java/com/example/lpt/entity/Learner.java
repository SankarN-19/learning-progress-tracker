package com.example.lpt.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "learners")
public class Learner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 100)
    private String cohort;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    protected Learner() {
    }

    public Learner(String displayName, String cohort, Account account) {
        this.displayName = displayName;
        this.cohort = cohort;
        this.account = account;
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getCohort() { return cohort; }
    public Account getAccount() { return account; }
}
