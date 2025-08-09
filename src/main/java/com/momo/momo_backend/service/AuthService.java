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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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

    /* -------------------------- 로그인 -------------------------- */
    public LoginResponse login(LoginRequest request) {
        UserCredential credential = credentialRepository.findByLoginId(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(request.getPassword(), credential.getPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken  = jwtTokenProvider.createAccessToken(credential.getLoginId());
        String refreshToken = jwtTokenProvider.createRefreshToken();

        return new LoginResponse(accessToken, refreshToken);
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
