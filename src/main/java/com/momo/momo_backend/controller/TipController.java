package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.TipRegisterRequest;
import com.momo.momo_backend.dto.TipRequest;
import com.momo.momo_backend.dto.TipResponse;
import com.momo.momo_backend.dto.TipUpdateRequest;
import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.service.TipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tips")
@RequiredArgsConstructor
public class TipController {

    private final TipService tipService;

    // 팁 생성
    @PostMapping("/generate")
    public ResponseEntity<Tip> generateTip(@RequestBody TipRequest tipRequest,
                                           @RequestParam Long userId) {
        Tip tip = tipService.createTip(userId, tipRequest);
        return ResponseEntity.ok(tip);
    }

    // 팁 등록
    @PostMapping("/register")
    public ResponseEntity<Tip> registerTip(@RequestBody TipRegisterRequest request) {
        Tip updated = tipService.registerTip(request.getTipId());
        return ResponseEntity.ok(updated);
    }


    // 팁 수정
    @PutMapping("/{no}")
    public ResponseEntity<TipResponse> update (
            @PathVariable Long no,
            @Valid @RequestBody TipUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Long userNo = userDetails.getUser().getNo(); // 기존 구성 활용
        return ResponseEntity.ok(tipService.update(no, userNo, request));
    }

    // 팁 삭제
    @DeleteMapping("/{no}")
    public ResponseEntity<Void> delete (
            @PathVariable Long no,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Long userNo = userDetails.getUser().getNo();
        tipService.delete(no, userNo);
        return ResponseEntity.noContent().build();
    }

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
    } // ✅ 닫는 중괄호 추가


    // ✅ 태그 기반 검색
    @GetMapping("/tag/{tagName}")
    public ResponseEntity<List<TipResponse>> getTipsByTag(@PathVariable String tagName){
        List<TipResponse> tips = tipService.getTipsByTag(tagName);
        return ResponseEntity.ok(tips);
    }

    // 특정 보관함의 꿀팁 조회
    @GetMapping("/storage/{storageId}")
    public ResponseEntity<List<TipResponse>> getTipsByStorage (@PathVariable Long storageId){
        return ResponseEntity.ok(tipService.getTipsByStorage(storageId));
    }


}
