package com.gymcrm.repository;

import com.gymcrm.domain.Member;
import com.gymcrm.domain.Member.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Page<Member> findAllByGymId(Long gymId, Pageable pageable);

    Optional<Member> findByIdAndGymId(Long id, Long gymId);

    long countByGymIdAndExpiryDateBefore(Long gymId, LocalDate date);

    long countByGymIdAndExpiryDate(Long gymId, LocalDate date);

    @Query("SELECT COALESCE(SUM(p.price), 0) FROM Member m JOIN m.plan p WHERE m.gymId = :gymId AND m.paymentStatus = :status")
    java.math.BigDecimal sumPlanPriceByGymIdAndPaymentStatus(@Param("gymId") Long gymId,
                                                              @Param("status") PaymentStatus status);

    @Query("SELECT m FROM Member m WHERE m.gymId = :gymId AND (m.expiryDate = :date1 OR m.expiryDate = :date2) ORDER BY m.expiryDate ASC")
    List<Member> findExpiringMembers(@Param("gymId") Long gymId,
                                     @Param("date1") LocalDate date1,
                                     @Param("date2") LocalDate date2);

    // Used by scheduler - spans all gyms
    List<Member> findByExpiryDateIn(List<LocalDate> dates);

    @Query("SELECT m FROM Member m WHERE m.gymId = :gymId AND (m.expiryDate <= :today OR m.paymentStatus = 'PENDING') ORDER BY m.expiryDate ASC")
    Page<Member> findDashboardMembers(@Param("gymId") Long gymId,
                                      @Param("today") LocalDate today,
                                      Pageable pageable);
}
