package com.example.lpt.repository;

import com.example.lpt.entity.Assignment;
import com.example.lpt.entity.AssignmentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    boolean existsByLearnerIdAndActivityId(Long learnerId, Long activityId);

    long countByStatus(AssignmentStatus status);

    long countByLearnerId(Long learnerId);

    long countByLearnerIdAndStatus(Long learnerId, AssignmentStatus status);

    /** Used when a learner is deleted. */
    @Modifying
    @Query("delete from Assignment a where a.learner.id = :learnerId")
    int deleteByLearnerId(@Param("learnerId") Long learnerId);

    /** Used when an activity is deleted. */
    @Modifying
    @Query("delete from Assignment a where a.activity.id = :activityId")
    int deleteByActivityId(@Param("activityId") Long activityId);

    @Query("select a from Assignment a join fetch a.activity where a.learner.id = :learnerId order by a.id")
    List<Assignment> findByLearnerIdWithActivity(@Param("learnerId") Long learnerId);

    @Query("select a.learner.id as learnerId, count(a) as total from Assignment a group by a.learner.id")
    List<LearnerAssignmentCount> countPerLearner();

    @Query("select a.learner.id as learnerId, count(a) as total from Assignment a "
            + "where a.status = :status group by a.learner.id")
    List<LearnerAssignmentCount> countPerLearnerWithStatus(@Param("status") AssignmentStatus status);
}
