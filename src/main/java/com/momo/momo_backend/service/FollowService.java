package com.momo.momo_backend.service;

import com.momo.momo_backend.entity.Follow;
import com.momo.momo_backend.entity.Notification;
import com.momo.momo_backend.entity.User;
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
    public void followUser(Long followerNo, String followeeLoginId) {
        User follower = userRepository.findById(followerNo)
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

        // ✅ 알림 전송: 현 Notification 엔티티는 (user, type, message, linkUrl, read, createdAt)
        //    - user: 수신자
        //    - type: 문자열로 관리 (예: "FOLLOWED_ME")
        //    - message/linkUrl: 선택
        Notification notification = Notification.of(
                following,                             // 수신자
                "FOLLOWED_ME",                         // 타입 문자열
                follower.getNickname() != null
                        ? follower.getNickname() + " 님이 나를 팔로우했습니다."
                        : follower.getLoginId() + " 님이 나를 팔로우했습니다.",
                null                                   // linkUrl (없으면 null)
        );
        notificationRepository.save(notification);
    }

    // 팔로우 취소
    @Transactional
    public void unfollowUser(Long followerNo, String followeeLoginId) {
        User follower = userRepository.findById(followerNo)
                .orElseThrow(() -> new IllegalArgumentException("언팔로우 요청자를 찾을 수 없습니다."));

        User following = userRepository.findByLoginId(followeeLoginId)
                .orElseThrow(() -> new IllegalArgumentException("언팔로우할 대상을 찾을 수 없습니다."));

        Follow follow = followRepository.findByFollowerAndFollowing(follower, following)
                .orElseThrow(() -> new IllegalArgumentException("팔로우 관계가 존재하지 않습니다."));

        followRepository.delete(follow);
        // 언팔로우 시 알림은 선택사항이므로 생략
    }
}
