package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.*;
import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.service.TipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tips")
@RequiredArgsConstructor
@Slf4j
public class TipController {

    private final TipService tipService;

    // 꿀팁 생성 (AI 요약 요청 및 임시 팁 저장)
    @PostMapping("/generate")
    public ResponseEntity<TipCreateResponse> createTip(@RequestBody TipCreateRequest request) {
        log.info("꿀팁 생성 요청 URL: {}", request.getUrl());
        try {
            TipCreateResponse response = tipService.createTip(request);
            log.info("꿀팁 생성 완료: {}", response.getTitle());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("꿀팁 생성 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<TipRegisterResponse> registerTip(@RequestBody @Valid TipRegisterRequest request,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        Long tempUserId = 1L;
        log.info("꿀팁 등록 요청: userId={}, title={}", tempUserId, request.getTitle());
        TipRegisterResponse response = tipService.registerTip(request, tempUserId);
        log.info("꿀팁 등록 완료: tipNo={}", response.getTipNo());
        return ResponseEntity.ok(response);
    }

    // 팁 수정
    @PutMapping("/{no}")
    public ResponseEntity<TipResponse> update (
            @PathVariable Long no,
            @Valid @RequestBody TipUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Long userNo = userDetails.getUser().getNo();
        return ResponseEntity.ok(tipService.update(no, userNo, request));
    }

    // 꿀팁 삭제
    @DeleteMapping("/{no}")
    public ResponseEntity<?> delete (
            @PathVariable Long no,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        try {
            Long userNo = userDetails.getUser().getNo();
            tipService.delete(no, userNo);
            log.info("꿀팁 삭제 성공: 꿀팁 ID {}", no);

            // 삭제 성공 메시지 반환
            MessageResponse response = MessageResponse.builder()
                    .message("꿀팁 삭제 완료.")
                    .build();
            return ResponseEntity.ok(response); // 200 OK와 메시지 반환
        } catch (AccessDeniedException e) {
            log.error("꿀팁 삭제 권한 없음: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (IllegalArgumentException e) {
            log.error("꿀팁 삭제 실패: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("꿀팁 삭제 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("꿀팁 삭제 중 오류가 발생했습니다.")
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
