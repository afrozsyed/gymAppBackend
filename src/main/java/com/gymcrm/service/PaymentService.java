package com.gymcrm.service;

import com.gymcrm.domain.Member;
import com.gymcrm.domain.Payment;
import com.gymcrm.domain.Plan;
import com.gymcrm.dto.request.PaymentRequest;
import com.gymcrm.dto.response.PaymentResponse;
import com.gymcrm.exception.ResourceNotFoundException;
import com.gymcrm.multitenancy.GymContext;
import com.gymcrm.repository.MemberRepository;
import com.gymcrm.repository.PaymentRepository;
import com.gymcrm.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MemberRepository memberRepository;
    private final PlanRepository planRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse recordPayment(Long memberId, PaymentRequest request) {
        Long gymId = GymContext.get();

        Member member = memberRepository.findByIdAndGymId(memberId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));

        Plan plan = planRepository.findByIdAndGymId(request.getPlanId(), gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + request.getPlanId()));

        // Update member: new plan, renewed expiry, mark PAID
        member.setPlan(plan);
        member.setExpiryDate(LocalDate.now().plusDays(plan.getDurationDays()));
        member.setPaymentStatus(Member.PaymentStatus.PAID);
        memberRepository.save(member);

        Payment payment = paymentRepository.save(Payment.builder()
                .gymId(gymId)
                .memberId(memberId)
                .planId(plan.getId())
                .amount(request.getAmount())
                .paymentMode(request.getPaymentMode())
                .notes(request.getNotes())
                .build());

        log.info("[PAYMENT] Recorded {} {} for member {} (plan: {}, expires: {})",
                payment.getPaymentMode(), payment.getAmount(),
                member.getName(), plan.getName(), member.getExpiryDate());

        return toResponse(payment, plan.getName());
    }

    public List<PaymentResponse> getPaymentHistory(Long memberId) {
        Long gymId = GymContext.get();

        if (!memberRepository.findByIdAndGymId(memberId, gymId).isPresent()) {
            throw new ResourceNotFoundException("Member not found: " + memberId);
        }

        List<Payment> payments = paymentRepository.findAllByGymIdAndMemberId(gymId, memberId);

        // Batch-fetch plan names for all payments that have a planId
        List<Long> planIds = payments.stream()
                .filter(p -> p.getPlanId() != null)
                .map(Payment::getPlanId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> planNames = planRepository.findAllById(planIds).stream()
                .collect(Collectors.toMap(Plan::getId, Plan::getName));

        return payments.stream()
                .map(p -> toResponse(p, p.getPlanId() != null ? planNames.get(p.getPlanId()) : null))
                .collect(Collectors.toList());
    }

    private PaymentResponse toResponse(Payment payment, String planName) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .paymentMode(payment.getPaymentMode())
                .notes(payment.getNotes())
                .paymentDate(payment.getPaymentDate())
                .status(payment.getStatus())
                .planName(planName)
                .build();
    }
}
