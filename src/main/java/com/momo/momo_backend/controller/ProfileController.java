package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.ErrorResponse;
import com.momo.momo_backend.dto.MessageResponse;
import com.momo.momo_backend.dto.ProfileImageUpdateResponse;
import com.momo.momo_backend.dto.ProfileUpdateRequest;
import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // 프로필 수정
    @PutMapping
    public ResponseEntity<?> updateUserProfile(
            @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            profileService.updateUserProfile(userDetails.getUser().getNo(), request);
            MessageResponse response = MessageResponse.builder()
                    .message("프로필이 성공적으로 수정되었습니다.")
                    .build();
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // 프로필 이미지 수정
    @PostMapping("/image")
    public ResponseEntity<?> updateUserProfileImage(
            @RequestParam("image") MultipartFile imageFile,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            ProfileImageUpdateResponse response = profileService.updateUserProfileImage(userDetails.getUser().getNo(), imageFile);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (RuntimeException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}