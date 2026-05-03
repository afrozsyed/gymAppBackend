package com.gymcrm.dto.request;

import com.gymcrm.domain.Member.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 30)
    private String phone;

    @NotNull(message = "Join date is required")
    private LocalDate joinDate;

    @NotNull(message = "Plan is required")
    private Long planId;

    @NotNull(message = "Payment status is required")
    private PaymentStatus paymentStatus;
}
