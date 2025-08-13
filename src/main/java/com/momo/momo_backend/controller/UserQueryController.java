package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.UserListResponse;
import com.momo.momo_backend.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserQueryController {

    private final UserQueryService userQueryService;

    // 모든 사용자 목록 조회
    @GetMapping("/all")
    public ResponseEntity<List<UserListResponse>> getAllUsers() {
        List<UserListResponse> users = userQueryService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // 사용자 아이디 검색
    @GetMapping("/search")
    public ResponseEntity<List<UserListResponse>> searchUsers(@RequestParam("id") String loginId) {
        List<UserListResponse> users = userQueryService.searchUsersByLoginId(loginId);
        return ResponseEntity.ok(users);
    }
}