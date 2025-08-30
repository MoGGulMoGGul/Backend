package com.momo.momo_backend.service;

import com.momo.momo_backend.entity.Follow;
import com.momo.momo_backend.entity.Notification;
import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.enums.NotificationType;
import com.momo.momo_backend.repository.FollowRepository;
import com.momo.momo_backend.repository.NotificationRepository;
import com.momo.momo_backend.repository.UserRepository;
import com.momo.momo_backend.realtime.events.NotificationCreatedEvent; // ✅ 추가
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;              // ✅ 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;                                             // ✅ 추가

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    private final ApplicationEventPublisher eventPublisher;          // ✅ 추가

    @Transactional
    public void followUser(Long followerNo, String followeeLoginId) {
        User follower = userRepository.findById(followerNo)
                .orElseThrow(() -> new IllegalArgumentException("팔로우 요청자를 찾을 수 없습니다."));
        User following = userRepository.findByLoginId(followeeLoginId)
                .orElseThrow(() -> new IllegalArgumentException("팔로우할 대상을 찾을 수 없습니다."));

        if (follower.getNo().equals(following.getNo())) {
            throw new IllegalArgumentException("자기 자신은 팔로우할 수 없습니다.");
        }

        if (followRepository.existsByFollowerAndFollowing(follower, following)) {
            throw new IllegalArgumentException("이미 팔로우한 사용자입니다.");
        }

        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();
        followRepository.save(follow);

        // 알림: 나를 팔로우함 (관련 팁 없음 → null)
        Notification notification = Notification.of(
                following,
                NotificationType.FOLLOWED_ME,
                null
        );
        notificationRepository.save(notification);

        // ✅ 저장 직후, 개인 큐 전송용 이벤트 발행
        String actorName = follower.getNickname() != null ? follower.getNickname() : follower.getLoginId();
        String message   = actorName + "님이 당신을 팔로우했습니다.";

        eventPublisher.publishEvent(
                new NotificationCreatedEvent(
                        following.getNo(),    // targetUserId
                        null,                 // tipId 없음
                        message,              // message
                        Instant.now()         // createdAt
                )
        );
    }

    @Transactional
    public void unfollowUser(Long followerNo, String followeeLoginId) {
        User follower = userRepository.findById(followerNo)
                .orElseThrow(() -> new IllegalArgumentException("언팔로우 요청자를 찾을 수 없습니다."));
        User following = userRepository.findByLoginId(followeeLoginId)
                .orElseThrow(() -> new IllegalArgumentException("언팔로우할 대상을 찾을 수 없습니다."));

        Follow follow = followRepository.findByFollowerAndFollowing(follower, following)
                .orElseThrow(() -> new IllegalArgumentException("팔로우 관계가 존재하지 않습니다."));

        followRepository.delete(follow);
    }
}
