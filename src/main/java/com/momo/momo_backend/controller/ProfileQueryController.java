package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.ErrorResponse;
import com.momo.momo_backend.dto.ProfileResponse;
import com.momo.momo_backend.service.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileQueryController {

    private final ProfileQueryService profileQueryService;

    @GetMapping("/{userNo}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userNo) {
        try {
            ProfileResponse response = profileQueryService.getUserProfile(userNo);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
}