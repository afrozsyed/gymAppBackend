package com.gymcrm.controller;

import com.gymcrm.dto.request.PaymentRequest;
import com.gymcrm.dto.response.PaymentResponse;
import com.gymcrm.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members/{memberId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> recordPayment(
            @PathVariable Long memberId,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.recordPayment(memberId, request));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(@PathVariable Long memberId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(memberId));
    }
}
