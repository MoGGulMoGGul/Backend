package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.UserListResponse;
import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
