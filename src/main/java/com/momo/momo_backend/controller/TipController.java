package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.*;
import com.momo.momo_backend.entity.Tip; // ✅ 상세 조회 시 엔티티 사용
import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.service.TipService;
import com.momo.momo_backend.service.TipQueryService; // ✅ 추가
import com.momo.momo_backend.realtime.service.TipViewsRankService; // ✅ 추가
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tips")
@RequiredArgsConstructor
@Slf4j
public class TipController {

    private final TipService tipService;
    private final TipQueryService tipQueryService;                 // ✅ 추가
    private final TipViewsRankService tipViewsRankService;         // ✅ 추가

    // ✅ 상세 조회 + 조회수 랭킹 기록
    @GetMapping("/{tipId}")
    public ResponseEntity<?> getOne(@PathVariable Long tipId) {
        try {
            Tip tip = tipQueryService.getTipDetails(tipId);
            tipViewsRankService.recordView(tipId); // ✅ 조회 기록 (Redis ZSET)
            return ResponseEntity.ok(TipResponse.from(tip));
        } catch (IllegalArgumentException e) {
            log.error("팁 상세 조회 실패: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            log.error("팁 상세 조회 중 오류: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("팁 상세 조회 중 오류가 발생했습니다.")
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 꿀팁 생성 (AI 요약 요청 및 임시 팁 저장)
    @PostMapping("/generate")
    public ResponseEntity<?> generateTip(@RequestBody TipRequest tipRequest,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("꿀팁 생성 요청 - 사용자: {}, URL: {}", userDetails.getUsername(), tipRequest.getUrl());
        try {
            Long userNo = userDetails.getUser().getNo();
            TipResponse tipResponse = tipService.createTip(userNo, tipRequest);
            log.info("꿀팁 생성 성공 - 꿀팁 ID: {}", tipResponse.getNo());
            return ResponseEntity.ok(tipResponse);
        } catch (IllegalArgumentException e) {
            log.error("꿀팁 생성 실패: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("꿀팁 생성 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("꿀팁 생성 중 오류가 발생했습니다.")
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 꿀팁 등록 (생성된 팁을 최종 보관함에 연결 및 공개 여부 설정)
    @PostMapping("/register")
    public ResponseEntity<?> registerTip(@RequestBody TipRegisterRequest request,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("꿀팁 등록 요청 - 꿀팁 ID: {}, 보관함 ID: {}, 공개 여부: {}, 사용자: {}",
                request.getTipId(), request.getStorageId(), request.getIsPublic(), userDetails.getUsername());
        try {
            Long userNo = userDetails.getUser().getNo(); // 등록하는 사용자 (팁 소유권 검증에 사용)
            TipResponse updatedTip = tipService.registerTip(request, userNo);
            log.info("꿀팁 등록 성공 - 꿀팁 ID: {}", updatedTip.getNo());
            return ResponseEntity.ok(updatedTip);
        } catch (IllegalArgumentException e) {
            log.error("꿀팁 등록 실패: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (AccessDeniedException e) {
            log.error("꿀팁 등록 권한 없음: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (Exception e) {
            log.error("꿀팁 등록 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("꿀팁 등록 중 오류가 발생했습니다.")
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
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

    // 팁 삭제
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
            return ResponseEntity.ok(response);
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

    // 태그 기반 검색
    @GetMapping("/tag/{tagName}")
    public ResponseEntity<List<TipResponse>> getTipsByTag(@PathVariable String tagName){
        List<TipResponse> tips = tipService.getTipsByTag(tagName);
        return ResponseEntity.ok(tips);
    }
}
