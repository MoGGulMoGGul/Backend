package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.UserListResponse;
import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;

    // 모든 사용자 조회
    public List<UserListResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> UserListResponse.builder()
                        .userNo(user.getNo())
                        .nickname(user.getNickname())
                        .profileImageUrl(user.getProfileImage())
                        .build())
                .collect(Collectors.toList());
    }

    // 사용자 아이디로 검색
    public List<UserListResponse> searchUsersByLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return Collections.emptyList(); // 검색어가 없으면 빈 리스트 반환
        }
        List<User> users = userRepository.findByLoginIdContainingIgnoreCase(loginId);
        return users.stream()
                .map(UserListResponse::from)
                .collect(Collectors.toList());
    }
}
