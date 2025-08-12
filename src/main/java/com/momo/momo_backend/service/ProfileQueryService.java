package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.ProfileResponse;
import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.repository.BookmarkRepository; // BookmarkRepository 임포트
import com.momo.momo_backend.repository.FollowRepository;
import com.momo.momo_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileQueryService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final BookmarkRepository bookmarkRepository; // 의존성 주입

    // 사용자 프로필 조회
    public ProfileResponse getUserProfile(Long userNo) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        long followerCount = followRepository.countByFollowing_No(userNo);
        long followingCount = followRepository.countByFollower_No(userNo);
        // 사용자가 작성한 모든 꿀팁이 즐겨찾기된 총 횟수를 계산
        long totalBookmarkCount = bookmarkRepository.countByTip_User_No(userNo);

        return ProfileResponse.from(user, followerCount, followingCount, totalBookmarkCount);
    }
}