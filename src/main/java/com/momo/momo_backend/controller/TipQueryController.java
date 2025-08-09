package com.momo.momo_backend.controller;

import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.service.TipQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/query/tips")
@RequiredArgsConstructor
public class TipQueryController {

    private final TipQueryService tipQueryService;

    // 사용자가 작성한 팁 조회
    @GetMapping("/my")
    public ResponseEntity<List<Tip>> getMyTips(@RequestParam Long userId) {
        List<Tip> tips = tipQueryService.getTipsByUser(userId);
        return ResponseEntity.ok(tips);
    }

    // 공개된 팁 목록 조회
    @GetMapping("/all")
    public ResponseEntity<List<Tip>> getAllPublicTips() {
        List<Tip> tips = tipQueryService.getAllPublicTips();
        return ResponseEntity.ok(tips);
    }

    // 특정 보관함에 속한 팁 조회
    @GetMapping("/storage/{storageNo}")
    public ResponseEntity<List<Tip>> getTipsByStorage(@PathVariable Long storageNo) {
        List<Tip> tips = tipQueryService.getTipsByStorage(storageNo);
        return ResponseEntity.ok(tips);
    }

    // 상세 팁 조회
    @GetMapping("/{tipId}")
    public ResponseEntity<Tip> getTipDetails(@PathVariable Long tipId) {
        Tip tip = tipQueryService.getTipDetails(tipId);
        return ResponseEntity.ok(tip);
    }
}