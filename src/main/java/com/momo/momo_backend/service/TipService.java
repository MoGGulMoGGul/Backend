package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.TipRequest;
import com.momo.momo_backend.dto.TipResponse;
import com.momo.momo_backend.dto.TipUpdateRequest;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.enums.NotificationType;
import com.momo.momo_backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TipService {

    private final TipRepository tipRepository;
    private final TagRepository tagRepository;
    private final TipTagRepository tipTagRepository;
    private final UserRepository userRepository;
    private final StorageRepository storageRepository;
    private final FollowRepository followRepository;
    private final NotificationRepository notificationRepository;
    private final StorageTipRepository storageTipRepository;
    private final GroupMemberRepository groupMemberRepository; // 그룹 멤버 리포지토리 추가

    // 팁 생성
    public TipResponse createTip(Long userNo, TipRequest request) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Storage storage = storageRepository.findById(request.getStorageId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 보관함입니다."));

        // 보관함 소유권 검증
        if (!storage.getUser().getNo().equals(userNo)) {
            throw new AccessDeniedException("선택한 보관함은 현재 로그인한 사용자의 소유가 아닙니다.");
        }
        // 그룹 보관함인 경우에도 그룹 멤버만 생성할 수 있도록 추가 검증 (StorageService의 create와 유사하게)
        if (storage.getGroup() != null) {
             boolean isGroupMember = groupMemberRepository.existsByGroupAndUser(storage.getGroup(), user);
             if (!isGroupMember) {
                 throw new AccessDeniedException("그룹 보관함에 꿀팁을 생성하려면 해당 그룹의 멤버여야 합니다.");
             }
        }


        Tip tip = new Tip();
        tip.setUser(user);
        tip.setUrl(request.getUrl());
        tip.setTitle(request.getTitle());
        tip.setIsPublic(request.getIsPublic());
        tip.setCreatedAt(LocalDateTime.now());
        tip.setUpdatedAt(LocalDateTime.now());

        Tip savedTip = tipRepository.save(tip); // 팁 저장

        // StorageTip 엔티티 생성 및 저장
        StorageTip storageTip = StorageTip.builder()
                .tip(savedTip)
                .storage(storage)
                .build();
        storageTipRepository.save(storageTip); // StorageTip 저장

        // 태그 처리 로직 추가
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            Set<String> uniqueTagNames = new LinkedHashSet<>(request.getTags());

            for (String tagName : uniqueTagNames) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));

                TipTag tipTag = TipTag.builder()
                        .tip(savedTip)
                        .tag(tag)
                        .build();
                tipTagRepository.save(tipTag); // 명시적으로 저장
                savedTip.getTipTags().add(tipTag);
            }
        }

        // 알림 전송 로직
        notifyFollowers(savedTip);
        notifyGroupMembers(savedTip);

        return TipResponse.from(savedTip);
    }

    // 전체 공개 팁 조회
    public List<TipResponse> getAllPublicTips() {
        return tipRepository.findAllByIsPublicTrueOrderByCreatedAtDesc()
                .stream().map(TipResponse::from).toList();
    }

    // 유저별 팁 조회
    public List<TipResponse> getTipsByUser(Long userNo) {
        return tipRepository.findAllByUser_NoOrderByCreatedAtDesc(userNo)
                .stream().map(TipResponse::from).toList();
    }

    // 그룹 보관함의 꿀팁 조회
    public List<TipResponse> getTipsByGroup(Long groupId) {
        return tipRepository.findTipsByGroupId(groupId)
                .stream().map(TipResponse::from).toList();
    }

    // 사용자 보관함 꿀팁 조회
    public List<TipResponse> getTipsInUserStorage(Long userId) {
        return tipRepository.findTipsByUserStorage(userId)
                .stream().map(TipResponse::from).toList();
    }

    // 태그 기반 검색
    public List<TipResponse> getTipsByTag(String tagName) {
        return tipRepository.findTipsByTagName(tagName)
                .stream().map(TipResponse::from).toList();
    }

    // 특정 보관함의 꿀팁 조회
    public List<TipResponse> getTipsByStorage(Long storageId) {
        return tipRepository.findTipsByStorageId(storageId)
                .stream().map(TipResponse::from).toList();
    }

    // 팁 등록 (FastAPI 서버에 요약 요청)
    public Tip registerTip(Long tipId) {
        Tip tip = tipRepository.findById(tipId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 tip입니다."));

        RestTemplate restTemplate = new RestTemplate();
        String pythonUrl = "http://localhost:8000/async-index/";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> request = Map.of("url", tip.getUrl());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(pythonUrl, entity, Map.class);
            String taskId = (String) response.getBody().get("task_id");

            tip.setContentSummary("요약 생성 중... (taskId: " + taskId + ")");
            tip.setUpdatedAt(LocalDateTime.now());
            return tipRepository.save(tip);
        } catch (Exception e) {
            throw new RuntimeException("FastAPI 요약 처리 요청 실패", e);
        }
    }

    // 팁 수정
    public TipResponse update(Long tipNo, Long userNo, TipUpdateRequest req) {
        Tip tip = tipRepository.findByNoAndUser_No(tipNo, userNo)
                .orElseThrow(() -> new AccessDeniedException("수정 권한이 없습니다."));

        if (req.getTitle() != null) tip.setTitle(req.getTitle());
        if (req.getContentSummary() != null) tip.setContentSummary(req.getContentSummary());
        if (req.getIsPublic() != null) tip.setIsPublic(req.getIsPublic());

        if (req.getTags() != null) {
            tipTagRepository.deleteByTipNo(tipNo);
            tip.getTipTags().clear();

            Set<String> names = new LinkedHashSet<>(req.getTags());

            for (String name : names) {
                Tag tag = tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
                TipTag tt = TipTag.builder().tip(tip).tag(tag).build();
                tip.getTipTags().add(tt);
            }
        }

        return TipResponse.from(tip);
    }

    // 팁 생성 시 알림과 함께 저장
    public Tip createTip(Tip tip) {
        Tip savedTip = tipRepository.save(tip);
        notifyFollowers(savedTip);
        notifyGroupMembers(savedTip);
        return savedTip;
    }

    // 팔로워에게 알림 전송
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

    // 그룹 멤버에게 알림 전송
    private void notifyGroupMembers(Tip savedTip) {
        List<Notification> notis = savedTip.getStorageTips().stream()
                .map(StorageTip::getStorage)
                .filter(s -> s.getGroup() != null)
                .flatMap(s -> s.getGroup().getGroupMembers().stream())
                .map(GroupMember::getUser)
                .filter(u -> !u.getNo().equals(savedTip.getUser().getNo()))
                .distinct()
                .map(user -> Notification.builder()
                        .receiver(user)
                        .tip(savedTip)
                        .type(NotificationType.GROUP_TIP_UPLOAD)
                        .isRead(false)
                        .build())
                .toList();

        if (!notis.isEmpty()) {
            notificationRepository.saveAll(notis);
        }
    }

    // 팁 삭제
    @Transactional
    public void delete(Long tipNo, Long userNo) {
        Tip tip = tipRepository.findByNoAndUser_No(tipNo, userNo)
                .orElseThrow(() -> new AccessDeniedException("삭제 권한이 없습니다."));

        tipTagRepository.deleteByTipNo(tipNo);
        tipRepository.delete(tip);
    }
}
