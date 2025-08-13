package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.FollowerResponse;
import com.momo.momo_backend.dto.FollowingResponse;
import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.service.FollowQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowQueryController {

    private final FollowQueryService followQueryService;

    // 자신을 팔로우한 계정 목록 조회 (팔로워 목록)
    @GetMapping("/followers")
    public ResponseEntity<List<FollowerResponse>> getFollowers(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userNo = userDetails.getUser().getNo();
        List<FollowerResponse> followers = followQueryService.getFollowers(userNo);
        return ResponseEntity.ok(followers);
    }

    // 자신이 팔로우한 계정 목록 조회 (팔로잉 목록)
    @GetMapping("/followings")
    public ResponseEntity<List<FollowingResponse>> getFollowings(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userNo = userDetails.getUser().getNo();
        List<FollowingResponse> followings = followQueryService.getFollowings(userNo);
        return ResponseEntity.ok(followings);
    }
}