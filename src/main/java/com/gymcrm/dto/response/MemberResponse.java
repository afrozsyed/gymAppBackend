package com.gymcrm.dto.response;

import com.gymcrm.domain.Member.PaymentStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MemberResponse {
    private Long id;
    private String name;
    private String phone;
    private LocalDate joinDate;
    private PlanResponse plan;
    private LocalDate expiryDate;
    private PaymentStatus paymentStatus;
    private String status;
    private LocalDateTime createdAt;
}
