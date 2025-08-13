package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.TipResponse;
import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.service.TipSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search/tips")
@RequiredArgsConstructor
public class TipSearchController {

    private final TipSearchService tipSearchService;

    /** 전체 공개 팁 검색 (등록된 것만) — 토큰 불필요 */
    @GetMapping("/public")
    public ResponseEntity<List<TipResponse>> searchPublic(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "OR") String mode, // OR | AND
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(tipSearchService.searchPublic(keyword, mode, page, size));
    }

    /** 내 전체 보관함 검색 — 인증 필요 */
    @GetMapping("/my")
    public ResponseEntity<List<TipResponse>> searchMy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "OR") String mode, // OR | AND
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userNo = userDetails.getUser().getNo();
        return ResponseEntity.ok(tipSearchService.searchMy(userNo, keyword, mode, page, size));
    }

    /** 그룹 보관함 검색 — 멤버만 접근 가능 */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<TipResponse>> searchGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "OR") String mode, // OR | AND
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userNo = userDetails.getUser().getNo();
        return ResponseEntity.ok(tipSearchService.searchGroup(groupId, userNo, keyword, mode, page, size));
    }

    /** 특정 보관함 검색 — 소유자 또는 그룹 멤버만 */
    @GetMapping("/storage/{storageId}")
    public ResponseEntity<List<TipResponse>> searchStorage(
            @PathVariable Long storageId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "OR") String mode, // OR | AND
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userNo = userDetails.getUser().getNo();
        return ResponseEntity.ok(tipSearchService.searchStorage(storageId, userNo, keyword, mode, page, size));
    }

    /** 태그 + 키워드 (공개 팁만) */
    @GetMapping("/tag/{tagName}")
    public ResponseEntity<List<TipResponse>> searchByTag(
            @PathVariable String tagName,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "OR") String mode, // OR | AND
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(tipSearchService.searchByTag(tagName, keyword, mode, page, size));
    }
}
