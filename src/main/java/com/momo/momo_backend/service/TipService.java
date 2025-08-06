package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.TipResponse;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.enums.NotificationType;
import com.momo.momo_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class TipService {

    private final TipRepository tipRepo;
    private final FollowRepository followRepo;
    private final NotificationRepository notiRepo;

    /* ------------------------------------------------- */
    /* 공통: 키워드 배열을 돌며 OR 결과를 합치는 유틸    */
    /* ------------------------------------------------- */
    private static List<Tip> mergeByKeywords(Function<String, List<Tip>> fn,
                                             String[] words) {
        return Arrays.stream(words)
                .flatMap(kw -> fn.apply(kw).stream())
                .distinct()
                .toList();
    }

    /* ---------------- 공개 꿀팁 ---------------- */
    public List<TipResponse> getAllPublicTips(String keyword) {
        if (keyword == null || keyword.isBlank())
            return tipRepo.findAllByIsPublicTrueOrderByCreatedAtDesc()
                    .stream().map(TipResponse::fromEntity).toList();

        String[] words = keyword.trim().split("\\s+");
        List<Tip> tips = mergeByKeywords(tipRepo::searchPublicTipsByKeyword, words);
        return tips.stream().map(TipResponse::fromEntity).toList();
    }

    /* ---------------- 내 전체 보관함 ---------------- */
    public List<TipResponse> getTipsInUserStorage(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank())
            return tipRepo.findTipsByUserStorage(userId)
                    .stream().map(TipResponse::fromEntity).toList();

        String[] words = keyword.trim().split("\\s+");
        List<Tip> tips = mergeByKeywords(
                kw -> tipRepo.searchUserStorageTipsByKeyword(userId, kw), words);
        return tips.stream().map(TipResponse::fromEntity).toList();
    }

    /* ---------------- 그룹 보관함 ---------------- */
    public List<TipResponse> getTipsByGroup(Long groupId, String keyword) {
        if (keyword == null || keyword.isBlank())
            return tipRepo.findTipsByGroupId(groupId)
                    .stream().map(TipResponse::fromEntity).toList();

        String[] words = keyword.trim().split("\\s+");
        List<Tip> tips = mergeByKeywords(
                kw -> tipRepo.searchGroupTipsByKeyword(groupId, kw), words);
        return tips.stream().map(TipResponse::fromEntity).toList();
    }

    /* ---------------- 특정 보관함 ---------------- */
    public List<TipResponse> getTipsByStorage(Long storageId, String keyword) {
        if (keyword == null || keyword.isBlank())
            return tipRepo.findTipsByStorageId(storageId)
                    .stream().map(TipResponse::fromEntity).toList();

        String[] words = keyword.trim().split("\\s+");
        List<Tip> tips = mergeByKeywords(
                kw -> tipRepo.searchStorageTipsByKeyword(storageId, kw), words);
        return tips.stream().map(TipResponse::fromEntity).toList();
    }

    /* ---------------- 태그 ---------------- */
    public List<TipResponse> getTipsByTag(String tagName, String keyword) {
        if (keyword == null || keyword.isBlank())
            return tipRepo.findTipsByTagName(tagName)
                    .stream().map(TipResponse::fromEntity).toList();

        String[] words = keyword.trim().split("\\s+");
        List<Tip> tips = mergeByKeywords(
                kw -> tipRepo.searchTagTipsByKeyword(tagName, kw), words);
        return tips.stream().map(TipResponse::fromEntity).toList();
    }

    /* ================================================================= */
    /* ▼ 이하: 생성 & 알림 로직                         */
    /* ================================================================= */

    @Transactional
    public Tip createTip(Tip tip) {
        Tip saved = tipRepo.save(tip);
        notifyFollowers(saved);
        notifyGroupMembers(saved);
        return saved;
    }

    /* 팔로워 알림 */
    private void notifyFollowers(Tip saved) {
        List<Notification> notis = followRepo.findByFollowing(saved.getUser())
                .stream()
                .map(f -> Notification.builder()
                        .receiver(f.getFollower())
                        .tip(saved)
                        .type(NotificationType.FOLLOWING_TIP_UPLOAD)
                        .isRead(false)
                        .build())
                .toList();
        notiRepo.saveAll(notis);
    }

    /* 그룹 멤버 알림 */
    private void notifyGroupMembers(Tip saved) {
        List<Notification> notis = saved.getStorageTips().stream()
                .map(StorageTip::getStorage)
                .filter(s -> s.getGroup() != null)
                .flatMap(s -> s.getGroup().getGroupMembers().stream())
                .map(GroupMember::getUser)
                .filter(u -> !u.getNo().equals(saved.getUser().getNo()))
                .distinct()
                .map(u -> Notification.builder()
                        .receiver(u)
                        .tip(saved)
                        .type(NotificationType.GROUP_TIP_UPLOAD)
                        .isRead(false)
                        .build())
                .toList();
        if (!notis.isEmpty()) notiRepo.saveAll(notis);
    }
}
