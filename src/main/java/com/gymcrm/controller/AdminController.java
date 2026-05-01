package com.gymcrm.controller;

import com.gymcrm.dto.request.CreateGymRequest;
import com.gymcrm.dto.request.ResetPasswordRequest;
import com.gymcrm.dto.response.GymDetailResponse;
import com.gymcrm.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/gyms")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping
    public ResponseEntity<GymDetailResponse> createGym(@Valid @RequestBody CreateGymRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createGym(request));
    }

    @GetMapping
    public ResponseEntity<List<GymDetailResponse>> getAllGyms() {
        return ResponseEntity.ok(adminService.getAllGyms());
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<GymDetailResponse> activateGym(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.activateGym(id));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<GymDetailResponse> deactivateGym(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deactivateGym(id));
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id,
                                              @Valid @RequestBody ResetPasswordRequest request) {
        adminService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
