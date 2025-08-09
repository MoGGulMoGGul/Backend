package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.*;
import com.momo.momo_backend.entity.Group;
import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Slf4j 임포트
import org.springframework.http.HttpStatus; // HttpStatus 임포트
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups") // 그룹 관련 API의 기본 경로
@RequiredArgsConstructor
@Slf4j // 로그 사용을 위해 추가
public class GroupController {

    private final GroupService groupService;

    // 그룹 생성 API
    @PostMapping
    public ResponseEntity<?> createGroup(
            @RequestBody GroupCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("그룹 생성 요청 - 그룹명: {}, 사용자: {}", request.getName(), userDetails.getUsername());
        try {
            Long userNo = userDetails.getUser().getNo(); // 현재 로그인한 사용자 번호
            Group createdGroup = groupService.createGroup(request, userNo);

            GroupCreateResponse response = GroupCreateResponse.builder()
                    .message("그룹 생성 완료!")
                    .groupNo(createdGroup.getNo())
                    .build();

            log.info("그룹 생성 성공 - 그룹 번호: {}", createdGroup.getNo());
            return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created 응답
        } catch (IllegalArgumentException e) {
            log.error("그룹 생성 실패: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value()) // 400 Bad Request
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("그룹 생성 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500 Internal Server Error
                    .message("그룹 생성 중 오류가 발생했습니다.")
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 그룹 멤버 초대 API
    @PostMapping("/{groupId}/invite") // 엔드포인트 수정: /api/groups/{groupId}/invite
    public ResponseEntity<?> inviteGroupMember(
            @PathVariable Long groupId,
            @RequestBody GroupInviteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("그룹 멤버 초대 요청 - 그룹 ID: {}, 초대할 사용자 ID: {}, 초대하는 사용자: {}",
                groupId, request.getUserId(), userDetails.getUsername());
        try {
            Long inviterUserNo = userDetails.getUser().getNo(); // 초대하는 사용자 (로그인된 사용자)
            groupService.inviteMember(groupId, request, inviterUserNo);

            MessageResponse response = MessageResponse.builder()
                    .message("그룹 멤버 초대 완료!")
                    .build();

            log.info("그룹 멤버 초대 성공 - 그룹 ID: {}, 초대된 사용자 ID: {}", groupId, request.getUserId());
            return ResponseEntity.ok(response); // 200 OK 응답
        } catch (IllegalArgumentException e) {
            log.error("그룹 멤버 초대 실패: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value()) // 400 Bad Request
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("그룹 멤버 초대 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500 Internal Server Error
                    .message("그룹 멤버 초대 중 오류가 발생했습니다.")
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 그룹 멤버 나가기 API
    @DeleteMapping("/{groupId}/leave") // 엔드포인트: /api/groups/{groupId}/leave
    public ResponseEntity<?> leaveGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("그룹 멤버 나가기 요청 - 그룹 ID: {}, 사용자: {}", groupId, userDetails.getUsername());
        try {
            Long userNo = userDetails.getUser().getNo(); // 현재 로그인한 사용자 번호
            groupService.leaveGroup(groupId, userNo);

            MessageResponse response = MessageResponse.builder()
                    .message("그룹에서 나갔습니다.")
                    .build();

            log.info("그룹 멤버 나가기 성공 - 그룹 ID: {}, 사용자: {}", groupId, userNo);
            return ResponseEntity.ok(response); // 200 OK 응답
        } catch (IllegalArgumentException e) {
            log.error("그룹 멤버 나가기 실패: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value()) // 400 Bad Request (그룹 없음, 멤버 아님 등)
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("그룹 멤버 나가기 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500 Internal Server Error
                    .message("그룹 멤버 나가기 중 오류가 발생했습니다.")
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 그룹 멤버 조회 API
    // 그룹 멤버 조회 API
    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> getGroupMembers(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("그룹 멤버 조회 요청 - 그룹 ID: {}, 요청 사용자: {}", groupId, userDetails.getUsername());
        try {
            Long requestingUserNo = userDetails.getUser().getNo(); // 요청하는 사용자 번호
            // ✨ Service 호출 시 requestingUserNo 전달
            List<User> members = groupService.getGroupMembers(groupId, requestingUserNo);

            List<GroupMemberResponse> responseList = members.stream()
                    .map(GroupMemberResponse::from)
                    .collect(Collectors.toList());

            log.info("그룹 멤버 조회 성공 - 그룹 ID: {}, 조회된 멤버 수: {}", groupId, responseList.size());
            return ResponseEntity.ok(responseList);
        } catch (IllegalArgumentException e) {
            log.error("그룹 멤버 조회 실패: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (AccessDeniedException e) { // AccessDeniedException 처리 추가
            log.error("그룹 멤버 조회 권한 없음: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.FORBIDDEN.value()) // 403 Forbidden
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (Exception e) {
            log.error("그룹 멤버 조회 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("그룹 멤버 조회 중 오류가 발생했습니다.")
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 그룹 목록 조회 API (사용자가 속한 그룹)
    @GetMapping("/check") // 엔드포인트: /api/groups/check
    public ResponseEntity<?> getGroupsForUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("그룹 목록 조회 요청 - 사용자: {}", userDetails.getUsername());
        try {
            Long userNo = userDetails.getUser().getNo();
            List<GroupListResponse> responseList = groupService.getGroupsForUser(userNo);

            log.info("그룹 목록 조회 성공 - 사용자: {}, 조회된 그룹 수: {}", userNo, responseList.size());
            return ResponseEntity.ok(responseList); // 200 OK 응답
        } catch (IllegalArgumentException e) {
            log.error("그룹 목록 조회 실패: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.value()) // 400 Bad Request (사용자 없음 등)
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("그룹 목록 조회 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("그룹 목록 조회 중 오류가 발생했습니다.")
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
