package com.momo.momo_backend.controller;

import com.momo.momo_backend.entity.Storage;
import com.momo.momo_backend.service.StorageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/query/storage")
@RequiredArgsConstructor
public class StorageQueryController {

    private final StorageQueryService storageQueryService;

    @GetMapping("/{userNo}")
    public ResponseEntity<List<Storage>> getStoragesByUser(@PathVariable Long userNo) {
        List<Storage> storages = storageQueryService.findByUser(userNo);
        return ResponseEntity.ok(storages);
    }
}