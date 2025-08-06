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
    public ResponseEntity<TipResponse> update(
            @PathVariable Long no,
            @Valid @RequestBody TipUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userNo = userDetails.getUser().getNo(); // 기존 구성 활용
        return ResponseEntity.ok(tipService.update(no, userNo, request));
    }

    // 팁 삭제
    @DeleteMapping("/{no}")
    public ResponseEntity<Void> delete(
            @PathVariable Long no,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userNo = userDetails.getUser().getNo();
        tipService.delete(no, userNo);
        return ResponseEntity.noContent().build();
    }
}
