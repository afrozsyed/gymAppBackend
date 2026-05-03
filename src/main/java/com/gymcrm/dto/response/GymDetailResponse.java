package com.gymcrm.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GymDetailResponse {
    private Long gymId;
    private String gymName;
    private String gymPhone;
    private String status;
    private String ownerName;
    private String ownerEmail;
    private String ownerPhone;
    private LocalDateTime createdAt;
}
