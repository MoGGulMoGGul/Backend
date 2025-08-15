package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.ErrorResponse;
import com.momo.momo_backend.dto.LoginRequest;
import com.momo.momo_backend.dto.LoginResponse;
import com.momo.momo_backend.dto.SignupRequest;
import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.security.JwtTokenProvider;
import com.momo.momo_backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    public record ResetPwRequest(String id, String nickname, String newPassword) {}
    public record FindIdRequest(String nickname, String password) {}

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
        authService.logout(accessToken); // 수정된 서비스 메서드 호출
        return ResponseEntity.ok().build();
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        String refreshToken = jwtTokenProvider.resolveToken(request);
        if (refreshToken == null) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .message("리프레시 토큰이 없습니다.")
                    .error("MissingTokenException")
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        try {
            LoginResponse newTokens = authService.refresh(refreshToken);
            return ResponseEntity.ok(newTokens);
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .message(e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
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

    // 회원탈퇴 (토큰 기반 유저 정보 사용)
    @DeleteMapping("/withdrawal")
    public ResponseEntity<Void> withdrawal(@AuthenticationPrincipal CustomUserDetails principal) {
        authService.withdraw(principal.getUser().getNo());
        return ResponseEntity.noContent().build(); // 204
    }

    @PostMapping("/find-id")
    public ResponseEntity<String> findId(@RequestBody FindIdRequest req) {
        String loginId = authService.findId(req.nickname(), req.password());
        return ResponseEntity.ok(loginId);
    }

    @PostMapping("/reset-pw")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPwRequest req) {
        authService.resetPassword(req.id(), req.nickname(), req.newPassword());
        return ResponseEntity.ok().build();
    }



}
