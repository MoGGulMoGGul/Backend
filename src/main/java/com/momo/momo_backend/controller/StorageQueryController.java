package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.StorageNameResponse; // 새로 추가된 DTO 임포트
import com.momo.momo_backend.dto.ErrorResponse; // ErrorResponse DTO 임포트
import com.momo.momo_backend.entity.Storage;
import com.momo.momo_backend.service.StorageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus; // HttpStatus 임포트
import lombok.extern.slf4j.Slf4j; // Slf4j 임포트

import java.util.List;
import java.util.stream.Collectors; // Collectors 임포트

@RestController
@RequestMapping("/api/query/storage")
@RequiredArgsConstructor
@Slf4j // 로그 사용을 위해 추가
public class StorageQueryController {

    private final StorageQueryService storageQueryService;

    @GetMapping("/{userNo}")
    public ResponseEntity<?> getStoragesByUser(@PathVariable Long userNo) { // 반환 타입을 ResponseEntity<?>로 변경
        log.info("보관함 목록 조회 요청 - userNo: {}", userNo);
        try {
            List<Storage> storages = storageQueryService.findByUser(userNo);

            // Storage 엔티티 리스트를 StorageNameResponse DTO 리스트로 변환
            List<StorageNameResponse> responseList = storages.stream()
                    .map(storage -> StorageNameResponse.builder()
                            .name(storage.getName())
                            .build())
                    .collect(Collectors.toList());

            log.info("보관함 목록 조회 성공 - userNo: {}, 조회된 보관함 수: {}", userNo, responseList.size());
            return ResponseEntity.ok(responseList);
        } catch (IllegalArgumentException e) {
            log.error("보관함 목록 조회 실패: {}", e.getMessage());
            // IllegalArgumentException 발생 시 ErrorResponse DTO를 반환
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.NOT_FOUND.value()) // 404 Not Found
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
}