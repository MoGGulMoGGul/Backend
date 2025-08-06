package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.TipResponse;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.enums.NotificationType;
import com.momo.momo_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TipService {

    private final TipRepository tipRepository;
    private final FollowRepository followRepository;
    private final NotificationRepository notificationRepository;

    /* ---------------- 조회 로직 ---------------- */

    public List<TipResponse> getAllPublicTips() {
        return tipRepository.findAllByIsPublicTrueOrderByCreatedAtDesc()
                .stream().map(TipResponse::fromEntity).toList();
    }

    public List<TipResponse> getTipsByUser(Long userNo) {
        return tipRepository.findAllByUser_NoOrderByCreatedAtDesc(userNo)
                .stream().map(TipResponse::fromEntity).toList();
    }

    public List<TipResponse> getTipsByGroup(Long groupId) {
        return tipRepository.findTipsByGroupId(groupId)
                .stream().map(TipResponse::fromEntity).toList();
    }

    public List<TipResponse> getTipsInUserStorage(Long userId) {
        return tipRepository.findTipsByUserStorage(userId)
                .stream().map(TipResponse::fromEntity).toList();
    }

    public List<TipResponse> getTipsByTag(String tagName) {
        return tipRepository.findTipsByTagName(tagName)
                .stream().map(TipResponse::fromEntity).toList();
    }

    public List<TipResponse> getTipsByStorage(Long storageId) {
        return tipRepository.findTipsByStorageId(storageId)
                .stream().map(TipResponse::fromEntity).toList();
    }

    /* ---------------- 생성 & 알림 ---------------- */

    @Transactional
    public Tip createTip(Tip tip) {
        Tip savedTip = tipRepository.save(tip);
        notifyFollowers(savedTip);      // 1) 팔로워 알림
        notifyGroupMembers(savedTip);   // 2) 그룹 구성원 알림
        return savedTip;
    }

    /** 1) 나를 팔로우한 모든 사용자에게 알림 */
    private void notifyFollowers(Tip savedTip) {
        List<Notification> notis = followRepository.findByFollowing(savedTip.getUser())
                .stream()
                .map(f -> Notification.builder()
                        .receiver(f.getFollower())
                        .tip(savedTip)
                        .type(NotificationType.FOLLOWING_TIP_UPLOAD)
                        .isRead(false)
                        .build())
                .toList();
        notificationRepository.saveAll(notis);
    }

    /** 2) ‘해당 꿀팁이 담긴 보관함’에 그룹이 있을 경우, 그 그룹의 멤버에게 알림 */
    private void notifyGroupMembers(Tip savedTip) {
        // Tip ↔ StorageTip ↔ Storage ↔ Group 순으로 타고 들어간다
        List<Notification> notis = savedTip.getStorageTips().stream()
                .map(StorageTip::getStorage)           // Storage
                .filter(s -> s.getGroup() != null)     // 그룹 보관함만
                .flatMap(s -> s.getGroup()
                        .getGroupMembers()      // List<GroupMember>
                        .stream())
                .map(GroupMember::getUser)             // List<User>
                .filter(u -> !u.getNo().equals(savedTip.getUser().getNo())) // 작성자 제외
                .distinct()
                .map(user -> Notification.builder()
                        .receiver(user)
                        .tip(savedTip)
                        .type(NotificationType.GROUP_TIP_UPLOAD)
                        .isRead(false)
                        .build())
                .toList();

        if (!notis.isEmpty()) notificationRepository.saveAll(notis);
    }
}
