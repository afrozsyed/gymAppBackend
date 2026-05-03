package com.gymcrm.service;

import com.gymcrm.domain.Member;
import com.gymcrm.domain.ReminderLog;
import com.gymcrm.exception.ResourceNotFoundException;
import com.gymcrm.multitenancy.GymContext;
import com.gymcrm.repository.MemberRepository;
import com.gymcrm.repository.ReminderLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final MemberRepository memberRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final MessagingService messagingService;

    @Transactional
    public void sendReminder(Long memberId) {
        Long gymId = GymContext.get();
        Member member = memberRepository.findByIdAndGymId(memberId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));

        messagingService.sendReminder(
                member.getPhone(),
                member.getName(),
                member.getExpiryDate().toString()
        );

        reminderLogRepository.save(ReminderLog.builder()
                .gymId(gymId)
                .memberId(memberId)
                .messageType("MANUAL_REMINDER")
                .sentAt(LocalDateTime.now())
                .build());
    }
}
