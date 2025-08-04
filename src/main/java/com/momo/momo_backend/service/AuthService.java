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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public void signup(SignupRequest request) {
        // 아이디 중복 확인
        if (userCredentialRepository.findById(request.getId()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // 사용자 생성 및 저장
        User user = new User();
        userRepository.save(user); // 먼저 저장 → no 생성됨

        // 사용자 인증 정보 생성 및 저장
        UserCredential credential = UserCredential.builder()
                .userNo(user.getNo())
                .id(request.getId())
                .pw(passwordEncoder.encode(request.getPassword()))
                .build();
        userCredentialRepository.save(credential);

        // 관계 설정 (선택적으로 setCredential 호출 가능)
    }

    public LoginResponse login(LoginRequest request) {
        UserCredential credential = userCredentialRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(request.getPassword(), credential.getPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        User user = userRepository.findById(credential.getUserNo())
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 일치하지 않습니다."));

        String accessToken = jwtTokenProvider.createAccessToken(credential.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken();

        return new LoginResponse(accessToken, refreshToken);
    }

    public boolean checkIdExists(String id) {
        return userCredentialRepository.findById(id).isPresent();
    }

    public boolean checkNicknameExists(String nickname) {
        return userRepository.findByNickname(nickname).isPresent();
    }
}
