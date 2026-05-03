package com.gymcrm.service;

import com.gymcrm.domain.Member;
import com.gymcrm.domain.ReminderLog;
import com.gymcrm.repository.MemberRepository;
import com.gymcrm.repository.ReminderLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final MemberRepository memberRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final MessagingService messagingService;

    // Runs every day at 08:00 — no GymContext needed (spans all gyms)
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendDailyExpiryReminders() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<Member> expiring = memberRepository.findByExpiryDateIn(List.of(today, tomorrow));
        log.info("[SCHEDULER] Daily reminder job: found {} expiring members", expiring.size());

        for (Member member : expiring) {
            messagingService.sendReminder(
                    member.getPhone(),
                    member.getName(),
                    member.getExpiryDate().toString()
            );

            reminderLogRepository.save(ReminderLog.builder()
                    .gymId(member.getGymId())
                    .memberId(member.getId())
                    .messageType("AUTO_EXPIRY_REMINDER")
                    .sentAt(LocalDateTime.now())
                    .build());
        }
    }
}
