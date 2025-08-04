package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.LoginRequest;
import com.momo.momo_backend.dto.LoginResponse;
import com.momo.momo_backend.dto.SignupRequest;
import com.momo.momo_backend.security.JwtTokenProvider;
import com.momo.momo_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String accessToken) {
        // 실제로는 refreshToken을 저장하고 있다면 무효화 필요
        return ResponseEntity.ok().build(); // 현재는 프론트에서 삭제 처리
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestHeader("Authorization") String refreshToken) {
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);  // 또는 redis 기반 식별
        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(); // 선택적: 새로 발급

        return ResponseEntity.ok(new LoginResponse(newAccessToken, newRefreshToken));
    }

    @GetMapping("/check-id")
    public ResponseEntity<Boolean> checkId(@RequestParam String id) {
        boolean exists = authService.checkIdExists(id);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        boolean exists = authService.checkNicknameExists(nickname);
        return ResponseEntity.ok(exists);
    }

}
