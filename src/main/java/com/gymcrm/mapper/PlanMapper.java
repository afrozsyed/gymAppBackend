package com.gymcrm.mapper;

import com.gymcrm.domain.Plan;
import com.gymcrm.dto.request.PlanRequest;
import com.gymcrm.dto.response.PlanResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlanMapper {

    PlanResponse toResponse(Plan plan);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gymId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Plan toEntity(PlanRequest request);
}
