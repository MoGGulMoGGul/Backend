package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.GroupStorageResponse;
import com.momo.momo_backend.dto.StorageNameResponse; // 새로 추가된 DTO 임포트
import com.momo.momo_backend.dto.ErrorResponse; // ErrorResponse DTO 임포트
import com.momo.momo_backend.entity.Storage;
import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.service.StorageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // 특정 사용자의 보관함 목록 조회 API
    @GetMapping("/{userNo}")
    public ResponseEntity<?> getStoragesByUser(@PathVariable Long userNo) {
        log.info("보관함 목록 조회 요청 - userNo: {}", userNo);
        try {
            List<Storage> storages = storageQueryService.findByUser(userNo);

            // StorageNameResponse DTO 리스트로 변환하며 필드 추가
            List<StorageNameResponse> responseList = storages.stream()
                    .map(storage -> StorageNameResponse.builder()
                            .storageNo(storage.getNo()) // storageNo 추가
                            .name(storage.getName())
                            .userNo(storage.getUser().getNo()) // userNo 추가
                            .build())
                    .collect(Collectors.toList());

            log.info("보관함 목록 조회 성공 - userNo: {}, 조회된 보관함 수: {}", userNo, responseList.size());
            return ResponseEntity.ok(responseList);
        } catch (IllegalArgumentException e) {
            log.error("보관함 목록 조회 실패: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.NOT_FOUND.value())
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    // 특정 그룹의 보관함 목록 조회 API
    @GetMapping("/group/{groupId}") // 엔드포인트: /api/query/storage/group/{groupId}
    public ResponseEntity<?> getGroupStorages(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) { // 인증된 사용자 정보 필요
        log.info("그룹 보관함 목록 조회 요청 - 그룹 ID: {}, 요청 사용자: {}", groupId, userDetails.getUsername());
        try {
            Long requestingUserNo = userDetails.getUser().getNo(); // 현재 로그인한 사용자 번호
            List<Storage> groupStorages = storageQueryService.findGroupStoragesByGroup(groupId, requestingUserNo);

            // Storage 엔티티 리스트를 GroupStorageResponse DTO 리스트로 변환
            List<GroupStorageResponse> responseList = groupStorages.stream()
                    .map(GroupStorageResponse::from)
                    .collect(Collectors.toList());

            log.info("그룹 보관함 목록 조회 성공 - 그룹 ID: {}, 조회된 보관함 수: {}", groupId, responseList.size());
            return ResponseEntity.ok(responseList); // 200 OK 응답
        } catch (IllegalArgumentException e) {
            log.error("그룹 보관함 목록 조회 실패: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value()) // 400 Bad Request (그룹 없음 등)
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (AccessDeniedException e) { // AccessDeniedException 처리 추가
            log.error("그룹 보관함 목록 조회 권한 없음: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.FORBIDDEN.value()) // 403 Forbidden
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (Exception e) {
            log.error("그룹 보관함 목록 조회 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500 Internal Server Error
                    .message("그룹 보관함 목록 조회 중 오류가 발생했습니다.")
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}