package com.gymcrm.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private long expiredCount;
    private long expiringToday;
    private BigDecimal pendingPayments;
    private long totalMembers;
    private List<MemberResponse> alertMembers;
}
