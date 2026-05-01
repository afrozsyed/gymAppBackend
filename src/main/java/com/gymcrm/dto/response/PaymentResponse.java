package com.gymcrm.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private BigDecimal amount;
    private String paymentMode;
    private String notes;
    private LocalDate paymentDate;
    private String status;
    private String planName;
}
