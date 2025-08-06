package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.TipResponse;
import com.momo.momo_backend.service.TipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.momo.momo_backend.security.CustomUserDetails;

import java.util.List;

@RestController
@RequestMapping("/api/tips")
@RequiredArgsConstructor
public class TipController {

    private final TipService tipService;

    // 전체 공개 꿀팁 조회
    @GetMapping("/public")
    public ResponseEntity<List<TipResponse>> getPublicTips(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(tipService.getAllPublicTips(keyword));
    }

    // 🔹 내 꿀팁 조회 API
    @GetMapping("/storage/my")
    public ResponseEntity<List<TipResponse>> getMyStorageTips(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String keyword      // ← 다중 키워드
    ) {
        return ResponseEntity.ok(
                tipService.getTipsInUserStorage(userDetails.getUser().getNo(), keyword));
    }

    // ✅ 그룹 보관함의 꿀팁 조회
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<TipResponse>> getTipsByGroup(
            @PathVariable Long groupId,
            @RequestParam(required = false) String keyword        // ★ 여러 단어
    ) {
        return ResponseEntity.ok(tipService.getTipsByGroup(groupId, keyword));
    }

    // ✅ 태그 기반 검색
    @GetMapping("/tag/{tagName}")
    public ResponseEntity<List<TipResponse>> getTipsByTag(
            @PathVariable String tagName,
            @RequestParam(required = false) String keyword   // ★ 여러 단어
    ) {
        return ResponseEntity.ok(tipService.getTipsByTag(tagName, keyword));
    }

     // 특정 보관함의 꿀팁 조회
     @GetMapping("/storage/{storageId}")
     public ResponseEntity<List<TipResponse>> getTipsByStorage(
             @PathVariable Long storageId,
             @RequestParam(required = false) String keyword      // ★ 여러 단어
     ) {
         return ResponseEntity.ok(
                 tipService.getTipsByStorage(storageId, keyword));
     }
}
