package com.gymcrm.repository;

import com.gymcrm.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByGymIdAndMemberId(Long gymId, Long memberId);
}
