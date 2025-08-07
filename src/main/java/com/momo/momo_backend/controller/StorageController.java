package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.StorageCreateRequest;
import com.momo.momo_backend.dto.StorageUpdateRequest;
import com.momo.momo_backend.entity.Storage;
import com.momo.momo_backend.service.StorageQueryService;
import com.momo.momo_backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;
    private final StorageQueryService storageQueryService;

    // 보관함 생성
    @PostMapping
    public ResponseEntity<Storage> create(@RequestBody StorageCreateRequest request) {
        Storage created = storageService.create(request);
        return ResponseEntity.ok(created);
    }

    // 보관함 수정
    @PutMapping("/{storageNo}")
    public ResponseEntity<Storage> updateStorage(
            @PathVariable Long storageNo,
            @RequestBody StorageUpdateRequest request) {
        Storage updated = storageService.update(storageNo, request);
        return ResponseEntity.ok(updated);
    }

    // 보관함 삭제
    @DeleteMapping("/{storageNo}")
    public ResponseEntity<Void> deleteStorage(@PathVariable Long storageNo) {
        storageService.delete(storageNo);
        return ResponseEntity.noContent().build();
    }
}
