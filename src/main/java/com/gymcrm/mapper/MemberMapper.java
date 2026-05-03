package com.gymcrm.mapper;

import com.gymcrm.domain.Member;
import com.gymcrm.dto.response.MemberResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.time.LocalDate;

@Mapper(componentModel = "spring", uses = {PlanMapper.class})
public interface MemberMapper {

    MemberResponse toResponse(Member member);

    @AfterMapping
    default void setStatus(Member member, @MappingTarget MemberResponse response) {
        LocalDate today = LocalDate.now();
        if (member.getExpiryDate().isBefore(today)) {
            response.setStatus("EXPIRED");
        } else if (member.getExpiryDate().isEqual(today)) {
            response.setStatus("EXPIRING_TODAY");
        } else {
            response.setStatus("ACTIVE");
        }
    }
}
