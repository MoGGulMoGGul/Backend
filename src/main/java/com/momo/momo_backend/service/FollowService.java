package com.momo.momo_backend.service;

import com.momo.momo_backend.entity.Follow;
import com.momo.momo_backend.entity.Notification;
import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.enums.NotificationType;
import com.momo.momo_backend.repository.FollowRepository;
import com.momo.momo_backend.repository.NotificationRepository;
import com.momo.momo_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void followUser(Long followerId, String followeeLoginId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new IllegalArgumentException("팔로우 요청자를 찾을 수 없습니다."));

        User following = userRepository.findByLoginId(followeeLoginId)
                .orElseThrow(() -> new IllegalArgumentException("팔로우할 대상을 찾을 수 없습니다."));

        if (follower.getNo().equals(following.getNo())) {
            throw new IllegalArgumentException("자기 자신은 팔로우할 수 없습니다.");
        }

        // 중복 팔로우 방지
        if (followRepository.existsByFollowerAndFollowing(follower, following)) {
            throw new IllegalArgumentException("이미 팔로우한 사용자입니다.");
        }

        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();
        followRepository.save(follow);

        // 알림 전송 (팔로우 당한 사람에게)
        Notification notification = Notification.builder()
                .receiver(following)
                .tip(null) // 팔로우는 특정 팁과 무관
                .type(NotificationType.FOLLOWED_ME)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    // 팔로우 취소 메서드 추가
    @Transactional
    public void unfollowUser(Long followerId, String followeeLoginId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new IllegalArgumentException("언팔로우 요청자를 찾을 수 없습니다."));

        User following = userRepository.findByLoginId(followeeLoginId)
                .orElseThrow(() -> new IllegalArgumentException("언팔로우할 대상을 찾을 수 없습니다."));

        Follow follow = followRepository.findByFollowerAndFollowing(follower, following)
                .orElseThrow(() -> new IllegalArgumentException("팔로우 관계가 존재하지 않습니다."));

        followRepository.delete(follow);
    }
}
