package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.*;
import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.realtime.service.TipViewsRankService;
import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.service.TipQueryService;
import com.momo.momo_backend.service.TipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tips")
@RequiredArgsConstructor
@Slf4j
public class TipController {

    private final TipService tipService;
    private final TipQueryService tipQueryService;
    private final TipViewsRankService tipViewsRankService;

    /** 상세 조회(+조회수 랭킹 기록) — 공개 */
    @GetMapping("/{tipId}")
    public ResponseEntity<?> getOne(@PathVariable Long tipId) {
        try {
            Tip tip = tipQueryService.getTipDetails(tipId);
            tipViewsRankService.recordView(tipId);
            return ResponseEntity.ok(TipResponse.from(tip));
        } catch (IllegalArgumentException e) {
            log.error("팁 상세 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .error(e.getClass().getSimpleName())
                            .build()
            );
        } catch (Exception e) {
            log.error("팁 상세 조회 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("팁 상세 조회 중 오류가 발생했습니다.")
                            .error(e.getClass().getSimpleName())
                            .build()
            );
        }
    }

    /** 꿀팁 생성 (AI 요약 요청 및 임시 저장) — 인증 필요 */
    @PostMapping("/generate")
    public ResponseEntity<?> generateTip(@RequestBody TipRequest tipRequest,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("꿀팁 생성 요청 - user:{}, url:{}", userDetails.getUsername(), tipRequest.getUrl());
        try {
            Long userNo = userDetails.getUser().getNo();
            // TipService는 createTip(loginUserNo, TipRequest) → generate(...) 호출 구조
            TipResponse tipResponse = tipService.createTip(userNo, tipRequest);
            return ResponseEntity.ok(tipResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .error(e.getClass().getSimpleName())
                            .build()
            );
        } catch (Exception e) {
            log.error("꿀팁 생성 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("꿀팁 생성 중 오류가 발생했습니다.")
                            .error(e.getClass().getSimpleName())
                            .build()
            );
        }
    }

    /** 꿀팁 최종 등록 — 인증 필요 */
    @PostMapping("/register")
    public ResponseEntity<?> registerTip(@Valid @RequestBody TipRegisterRequest request,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("꿀팁 등록 요청 - tip:{}, storage:{}, public:{}, user:{}",
                request.getTipNo(), request.getStorageNo(), request.getIsPublic(), userDetails.getUsername());
        try {
            Long userNo = userDetails.getUser().getNo();
            TipResponse updatedTip = tipService.registerTip(request, userNo);
            return ResponseEntity.ok(updatedTip);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .error(e.getClass().getSimpleName())
                            .build()
            );
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.FORBIDDEN.value())
                            .message(e.getMessage())
                            .error(e.getClass().getSimpleName())
                            .build()
            );
        } catch (Exception e) {
            log.error("꿀팁 등록 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("꿀팁 등록 중 오류가 발생했습니다.")
                            .error(e.getClass().getSimpleName())
                            .build()
            );
        }
    }

    /** 팁 수정 — 인증 필요 */
    @PutMapping("/{no}")
    public ResponseEntity<TipResponse> update(@PathVariable Long no,
                                              @Valid @RequestBody TipUpdateRequest request,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userNo = userDetails.getUser().getNo();
        return ResponseEntity.ok(tipService.update(no, userNo, request));
    }

    /** 팁 삭제 — 인증 필요 */
    @DeleteMapping("/{no}")
    public ResponseEntity<?> delete(@PathVariable Long no,
                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long userNo = userDetails.getUser().getNo();
            tipService.delete(no, userNo);
            return ResponseEntity.ok(MessageResponse.builder().message("꿀팁 삭제 완료.").build());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.FORBIDDEN.value())
                            .message(e.getMessage())
                            .error(e.getClass().getSimpleName())
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .error(e.getClass().getSimpleName())
                            .build()
            );
        } catch (Exception e) {
            log.error("꿀팁 삭제 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("꿀팁 삭제 중 오류가 발생했습니다.")
                            .error(e.getClass().getSimpleName())
                            .build()
            );
        }
    }

    /** AI 꿀팁 자동 생성/등록(비동기) — 인증 필요 */
    @PostMapping("/auto-create-async")
    public ResponseEntity<?> createAndRegisterTipAutoAsync(@RequestBody TipAutoCreateRequest request,
                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long userNo = userDetails.getUser().getNo();
            TipResponse tipResponse = tipService.createAndRegisterTipAutoAsync(userNo, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(tipResponse);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.FORBIDDEN.value())
                            .message(e.getMessage())
                            .error(e.getClass().getSimpleName())
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .error(e.getClass().getSimpleName())
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message(e.getMessage())
                            .error(e.getClass().getSimpleName())
                            .build()
            );
        }
    }
}
