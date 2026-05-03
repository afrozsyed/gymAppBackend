package com.gymcrm.service;

import com.gymcrm.domain.Member;
import com.gymcrm.domain.Plan;
import com.gymcrm.dto.request.MemberRequest;
import com.gymcrm.dto.response.MemberResponse;
import com.gymcrm.exception.ResourceNotFoundException;
import com.gymcrm.mapper.MemberMapper;
import com.gymcrm.multitenancy.GymContext;
import com.gymcrm.repository.MemberRepository;
import com.gymcrm.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PlanRepository planRepository;
    private final MemberMapper memberMapper;

    public Page<MemberResponse> getAll(Pageable pageable) {
        Long gymId = GymContext.get();
        return memberRepository.findAllByGymId(gymId, pageable)
                .map(memberMapper::toResponse);
    }

    public MemberResponse getById(Long id) {
        Long gymId = GymContext.get();
        Member member = memberRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + id));
        return memberMapper.toResponse(member);
    }

    @Transactional
    public MemberResponse create(MemberRequest request) {
        Long gymId = GymContext.get();
        Plan plan = planRepository.findByIdAndGymId(request.getPlanId(), gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + request.getPlanId()));

        Member member = Member.builder()
                .gymId(gymId)
                .name(request.getName())
                .phone(request.getPhone())
                .joinDate(request.getJoinDate())
                .plan(plan)
                .expiryDate(request.getJoinDate().plusDays(plan.getDurationDays()))
                .paymentStatus(request.getPaymentStatus())
                .build();

        return memberMapper.toResponse(memberRepository.save(member));
    }

    @Transactional
    public MemberResponse update(Long id, MemberRequest request) {
        Long gymId = GymContext.get();
        Member member = memberRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + id));

        Plan plan = planRepository.findByIdAndGymId(request.getPlanId(), gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + request.getPlanId()));

        member.setName(request.getName());
        member.setPhone(request.getPhone());
        member.setJoinDate(request.getJoinDate());
        member.setPlan(plan);
        member.setExpiryDate(request.getJoinDate().plusDays(plan.getDurationDays()));
        member.setPaymentStatus(request.getPaymentStatus());

        return memberMapper.toResponse(memberRepository.save(member));
    }

    @Transactional
    public void delete(Long id) {
        Long gymId = GymContext.get();
        Member member = memberRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + id));
        memberRepository.delete(member);
    }
}
