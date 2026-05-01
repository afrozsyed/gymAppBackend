package com.gymcrm.repository;

import com.gymcrm.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findAllByGymId(Long gymId);
    Optional<Plan> findByIdAndGymId(Long id, Long gymId);
}
