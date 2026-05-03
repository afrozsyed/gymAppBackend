package com.gymcrm.service;

import com.gymcrm.domain.Plan;
import com.gymcrm.dto.request.PlanRequest;
import com.gymcrm.dto.response.PlanResponse;
import com.gymcrm.exception.ResourceNotFoundException;
import com.gymcrm.mapper.PlanMapper;
import com.gymcrm.multitenancy.GymContext;
import com.gymcrm.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanMapper planMapper;

    public List<PlanResponse> getAll() {
        Long gymId = GymContext.get();
        return planRepository.findAllByGymId(gymId)
                .stream()
                .map(planMapper::toResponse)
                .toList();
    }

    @Transactional
    public PlanResponse create(PlanRequest request) {
        Long gymId = GymContext.get();
        Plan plan = planMapper.toEntity(request);
        plan.setGymId(gymId);
        return planMapper.toResponse(planRepository.save(plan));
    }

    public PlanResponse getById(Long id) {
        Long gymId = GymContext.get();
        Plan plan = planRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + id));
        return planMapper.toResponse(plan);
    }

    @Transactional
    public void delete(Long id) {
        Long gymId = GymContext.get();
        Plan plan = planRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + id));
        planRepository.delete(plan);
    }
}
