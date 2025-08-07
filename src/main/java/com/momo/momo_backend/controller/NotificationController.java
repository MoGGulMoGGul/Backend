package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.NotificationResponse;
import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NotificationResponse> list = notificationService.getNotifications(userDetails.getUser().getNo());
        return ResponseEntity.ok(list);
    }
}
