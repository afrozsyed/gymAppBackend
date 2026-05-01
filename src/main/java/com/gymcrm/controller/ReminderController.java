package com.gymcrm.controller;

import com.gymcrm.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping("/send/{memberId}")
    public ResponseEntity<Map<String, String>> sendReminder(@PathVariable Long memberId) {
        reminderService.sendReminder(memberId);
        return ResponseEntity.ok(Map.of("message", "Reminder sent successfully"));
    }
}
