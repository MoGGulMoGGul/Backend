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
    public ResponseEntity<List<TipResponse>> getPublicTips() {
        List<TipResponse> tips = tipService.getAllPublicTips();
        return ResponseEntity.ok(tips);
    }

    // 🔹 내 꿀팁 조회 API
    @GetMapping("/storage/my")
    public ResponseEntity<List<TipResponse>> getMyStorageTips(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                tipService.getTipsInUserStorage(userDetails.getUser().getNo()));
    }

    // ✅ 그룹 보관함의 꿀팁 조회
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<TipResponse>> getTipsByGroup(@PathVariable Long groupId) {
        List<TipResponse> tips = tipService.getTipsByGroup(groupId);
        return ResponseEntity.ok(tips);
    }

    // ✅ 태그 기반 검색
    @GetMapping("/tag/{tagName}")
    public ResponseEntity<List<TipResponse>> getTipsByTag(@PathVariable String tagName) {
        List<TipResponse> tips = tipService.getTipsByTag(tagName);
        return ResponseEntity.ok(tips);
    }

     // 특정 보관함의 꿀팁 조회
    @GetMapping("/storage/{storageId}")
    public ResponseEntity<List<TipResponse>> getTipsByStorage(@PathVariable Long storageId) {
        return ResponseEntity.ok(tipService.getTipsByStorage(storageId));
    }
}
