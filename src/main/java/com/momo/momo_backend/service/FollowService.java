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

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public void followUser(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) throw new IllegalArgumentException("자기 자신은 팔로우할 수 없습니다.");

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new IllegalArgumentException("팔로우 요청자 없음"));

        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new IllegalArgumentException("팔로우 대상 없음"));

        // 중복 팔로우 방지
        if (followRepository.existsByFollowerAndFollowing(follower, following)) return;

        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();
        followRepository.save(follow);

        // 알림 전송
        Notification notification = Notification.builder()
                .receiver(following)
                .tip(null) // 팔로우는 Tip과 무관
                .type(NotificationType.FOLLOWED_ME)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }
}
