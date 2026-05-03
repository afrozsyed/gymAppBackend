package com.gymcrm.service;

import com.gymcrm.domain.Member.PaymentStatus;
import com.gymcrm.dto.response.DashboardResponse;
import com.gymcrm.dto.response.MemberResponse;
import com.gymcrm.mapper.MemberMapper;
import com.gymcrm.multitenancy.GymContext;
import com.gymcrm.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    public DashboardResponse getDashboard() {
        Long gymId = GymContext.get();
        LocalDate today = LocalDate.now();

        long expiredCount = memberRepository.countByGymIdAndExpiryDateBefore(gymId, today);
        long expiringToday = memberRepository.countByGymIdAndExpiryDate(gymId, today);

        BigDecimal pendingPayments = memberRepository
                .sumPlanPriceByGymIdAndPaymentStatus(gymId, PaymentStatus.PENDING);

        long totalMembers = memberRepository.findAllByGymId(gymId, PageRequest.of(0, 1)).getTotalElements();

        List<MemberResponse> alertMembers = memberRepository
                .findDashboardMembers(gymId, today, PageRequest.of(0, 20))
                .stream()
                .map(memberMapper::toResponse)
                .toList();

        return DashboardResponse.builder()
                .expiredCount(expiredCount)
                .expiringToday(expiringToday)
                .pendingPayments(pendingPayments != null ? pendingPayments : BigDecimal.ZERO)
                .totalMembers(totalMembers)
                .alertMembers(alertMembers)
                .build();
    }
}
