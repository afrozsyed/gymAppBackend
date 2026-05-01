package com.gymcrm.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlanResponse {
    private Long id;
    private String name;
    private Integer durationDays;
    private BigDecimal price;
}
