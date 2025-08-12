package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.FollowerResponse;
import com.momo.momo_backend.dto.FollowingResponse;
import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.repository.FollowRepository;
import com.momo.momo_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowQueryService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    // 나를 팔로우하는 사용자 목록(팔로워) 조회
    public List<FollowerResponse> getFollowers(Long userNo) {
        // 토큰 기반으로 조회하므로, user가 존재하지 않는 경우는 거의 없지만 방어 로직 추가
        if (!userRepository.existsById(userNo)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        List<User> followers = followRepository.findFollowersByUserNo(userNo);
        return followers.stream()
                .map(FollowerResponse::from)
                .collect(Collectors.toList());
    }

    // 내가 팔로우하는 사용자 목록(팔로잉) 조회
    public List<FollowingResponse> getFollowings(Long userNo) {
        if (!userRepository.existsById(userNo)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        List<User> followings = followRepository.findFollowingByUserNo(userNo);
        return followings.stream()
                .map(FollowingResponse::from)
                .collect(Collectors.toList());
    }
}