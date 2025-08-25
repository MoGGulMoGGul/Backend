package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.LoginRequest;
import com.momo.momo_backend.dto.LoginResponse;
import com.momo.momo_backend.dto.SignupRequest;
import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.entity.UserCredential;
import com.momo.momo_backend.repository.UserRepository;
import com.momo.momo_backend.repository.UserCredentialRepository;
import com.momo.momo_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    /* ------------------------- 회원가입 ------------------------- */
    public void signup(SignupRequest request) {
        if (credentialRepository.findByLoginId(request.getId()).isPresent()
                || userRepository.findByLoginId(request.getId()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        String encoded = passwordEncoder.encode(request.getPassword());

        User user = userRepository.save(
                User.builder()
                        .loginId(request.getId())
                        .password(encoded)  // users.password
                        .nickname(request.getNickname())
                        .build()
        );

        UserCredential credential = UserCredential.builder()
                .user(user)
                .loginId(request.getId())
                .pw(encoded)
                .build();

        credentialRepository.save(credential);
    }

    // 로그아웃 로직
    public void logout(String accessToken) {
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        // 1. 엑세스 토큰을 남은 유효시간만큼 블랙리스트에 추가
        Date expiration = jwtTokenProvider.getExpirationDateFromToken(accessToken);
        long now = new Date().getTime();
        long remainingValidity = expiration.getTime() - now;
        if (remainingValidity > 0) {
            redisTemplate.opsForValue().set(accessToken, "logout", remainingValidity, TimeUnit.MILLISECONDS);
        }

        // 2. Redis에서 해당 유저의 리프레시 토큰을 삭제하여 무효화
        String userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        redisTemplate.delete("RT:" + userId);
    }


    // 리프레시 토큰을 사용하여 새로운 토큰 생성
    @Transactional
    public LoginResponse refresh(String refreshToken) {
        // 1. 토큰 기본 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 2. 토큰에서 사용자 ID 추출
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        if (userId == null) {
            throw new IllegalArgumentException("리프레시 토큰에서 사용자 정보를 찾을 수 없습니다.");
        }

        // 3. Redis에 저장된 유효한 리프레시 토큰과 일치하는지 확인
        String storedRefreshToken = (String) redisTemplate.opsForValue().get("RT:" + userId);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new IllegalArgumentException("리프레시 토큰이 일치하지 않거나 만료되었습니다. 다시 로그인해주세요.");
        }

        // 4. 새로운 토큰 쌍 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        // 5. Redis에 새로운 리프레시 토큰으로 업데이트 (기존 토큰은 자동으로 무효화됨)
        redisTemplate.opsForValue().set(
                "RT:" + userId,
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenValidity(),
                TimeUnit.MILLISECONDS
        );

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    /* -------------------------- 로그인 -------------------------- */
    public LoginResponse login(LoginRequest request) {
        UserCredential credential = credentialRepository.findByLoginId(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(request.getPassword(), credential.getPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        User user = credential.getUser();

        String accessToken = jwtTokenProvider.createAccessToken(credential.getLoginId());
        String refreshToken = jwtTokenProvider.createRefreshToken(credential.getLoginId());

        // ✅ 응답에 userNo 포함
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userNo(user.getNo())
                .build();
    }

    /* -------------------------- 회원탈퇴 -------------------------- */
    @Transactional
    public void withdraw(Long userNo) {
        credentialRepository.deleteByUser_No(userNo);
        userRepository.deleteById(userNo);
    }

    /* -------------------- 중복 체크 -------------------- */
    public boolean checkIdExists(String id) {
        return credentialRepository.findByLoginId(id).isPresent();
    }

    public boolean checkNicknameExists(String nickname) {
        return userRepository.findByNickname(nickname).isPresent();
    }

    public String findId(String nickname, String password) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("해당 닉네임의 사용자가 존재하지 않습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return user.getLoginId(); // loginId 반환
    }

    /* -------------------- 비밀번호 재설정 -------------------- */
    public void resetPassword(String id, String nickname, String newPassword) {
        UserCredential credential = credentialRepository.findByLoginId(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 ID입니다."));

        User user = credential.getUser();

        if (!user.getNickname().equals(nickname)) {
            throw new IllegalArgumentException("닉네임이 일치하지 않습니다.");
        }

        String encoded = passwordEncoder.encode(newPassword);

        // User 테이블 비밀번호 업데이트
        user.setPassword(encoded);
        userRepository.save(user);

        // Credential 테이블 비밀번호 업데이트
        credential.setPw(encoded);
        credentialRepository.save(credential);
    }

}
