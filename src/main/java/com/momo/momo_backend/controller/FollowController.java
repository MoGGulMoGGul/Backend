package com.momo.momo_backend.controller;

import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{targetUserId}")
    public ResponseEntity<Void> follow(@PathVariable Long targetUserId,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        followService.followUser(userDetails.getUser().getNo(), targetUserId);
        return ResponseEntity.ok().build();
    }
}
