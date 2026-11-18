package com.example.lpt.repository;

import com.example.lpt.entity.Learner;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LearnerRepository extends JpaRepository<Learner, Long> {

    @Query("select l from Learner l join fetch l.account order by l.id")
    List<Learner> findAllWithAccount();

    Optional<Learner> findByAccountId(Long accountId);
}
