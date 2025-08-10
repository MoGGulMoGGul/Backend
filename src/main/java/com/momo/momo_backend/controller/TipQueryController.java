package com.momo.momo_backend.controller;

import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.dto.TipResponse; // TipResponse DTO 임포트
import com.momo.momo_backend.service.TipQueryService;
import com.momo.momo_backend.security.CustomUserDetails; // CustomUserDetails 임포트
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // AuthenticationPrincipal 임포트
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors; // Collectors 임포트

@RestController
@RequestMapping("/api/query/tips")
@RequiredArgsConstructor
public class TipQueryController {

    private final TipQueryService tipQueryService;

    // 사용자가 작성한 팁 조회 (등록된 팁만) - 토큰 필요
    @GetMapping("/my")
    public ResponseEntity<List<TipResponse>> getMyTips(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getNo(); // 로그인된 사용자의 ID를 토큰에서 가져옴
        List<Tip> tips = tipQueryService.getTipsByUser(userId);
        List<TipResponse> responseList = tips.stream()
                .map(TipResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    // 공개된 팁 목록 조회 (등록된 팁만) - 토큰 불필요 (SecurityConfig에서 permitAll)
    @GetMapping("/all")
    public ResponseEntity<List<TipResponse>> getAllPublicTips() {
        List<Tip> tips = tipQueryService.getAllPublicTips();
        List<TipResponse> responseList = tips.stream()
                .map(TipResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    // 특정 보관함에 속한 팁 조회 (등록된 팁만) - 토큰 필요
    @GetMapping("/storage/{storageNo}")
    public ResponseEntity<List<TipResponse>> getTipsByStorage(@PathVariable Long storageNo,
                                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 이 메서드는 @AuthenticationPrincipal을 통해 인증된 사용자만 접근 가능합니다.
        // 추가적인 비즈니스 로직 검증은 TipQueryService에서 수행될 수 있습니다.
        List<Tip> tips = tipQueryService.getTipsByStorage(storageNo);
        List<TipResponse> responseList = tips.stream()
                .map(TipResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    // 상세 팁 조회 - 토큰 불필요 (SecurityConfig에서 permitAll)
    @GetMapping("/{tipId}")
    public ResponseEntity<TipResponse> getTipDetails(@PathVariable Long tipId) {
        Tip tip = tipQueryService.getTipDetails(tipId);
        TipResponse response = TipResponse.from(tip);
        return ResponseEntity.ok(response);
    }
}
