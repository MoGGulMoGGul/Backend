package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.*;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.enums.NotificationType;
import com.momo.momo_backend.realtime.events.NotificationCreatedEvent;
import com.momo.momo_backend.realtime.service.TipViewsRankService;
import com.momo.momo_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipService {

    @Value("${ai.server.url}")
    private String aiApiUrl;

    private final TipRepository tipRepository;
    private final UserRepository userRepository;
    private final StorageRepository storageRepository;
    private final StorageTipRepository storageTipRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final TagRepository tagRepository;
    private final TipTagRepository tipTagRepository;
    private final BookmarkRepository bookmarkRepository;

    // 알림용
    private final FollowRepository followRepository;
    private final NotificationRepository notificationRepository;

    private final RestTemplate restTemplate;
    private final TipViewsRankService tipViewsRankService;     // 조회수 랭킹 (미사용이면 추후 정리 가능)
    private final SimpMessagingTemplate messagingTemplate;     // 공개 피드 브로드캐스트
    private final ApplicationEventPublisher eventPublisher;    // 개인 큐 푸시(이벤트)

    /* =========================================================
       1) 생성 (초안 저장 + 요약 비동기 요청)
       ========================================================= */
    @Transactional
    public TipResponse createTip(Long loginUserNo, TipRequest request) {
        return generate(loginUserNo, request);
    }

    @Transactional
    public TipResponse generate(Long loginUserNo, TipRequest request) {
        if (request == null || !StringUtils.hasText(request.getUrl())) {
            throw new IllegalArgumentException("url은 필수입니다.");
        }

        User author = userRepository.findById(loginUserNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Tip tip = new Tip();
        tip.setUser(author);
        tip.setUrl(request.getUrl());
        tip.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : "제목 없음");
        tip.setIsPublic(false);
        tip.setCreatedAt(LocalDateTime.now());
        tip.setUpdatedAt(LocalDateTime.now());

        String taskId = requestSummaryAsync(request.getUrl());
        tip.setContentSummary(taskId != null
                ? "요약 생성 중... (taskId: " + taskId + ")"
                : "요약 대기 중...");

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            List<Tag> tags = upsertTags(request.getTags());
            List<TipTag> tipTags = tags.stream().map(t -> {
                TipTag tt = new TipTag();
                tt.setTip(tip);
                tt.setTag(t);
                return tt;
            }).collect(Collectors.toList());
            tip.setTipTags(tipTags);
        }

        Tip saved = tipRepository.save(tip);

        // 공개 피드(전체 구독 주제) 브로드캐스트
        Map<String, Object> feed = Map.of(
                "type", "tip:new",
                "tipId", saved.getNo(),
                "author", Optional.ofNullable(saved.getUser().getNickname()).orElse(saved.getUser().getLoginId()),
                "tags", Optional.ofNullable(saved.getTipTags()).orElse(List.of()).stream()
                        .map(tt -> tt.getTag().getName()).toList(),
                "createdAt", saved.getCreatedAt(),
                "v", "v1"
        );
        messagingTemplate.convertAndSend("/topic/feed", feed);

        log.info("꿀팁 생성 완료(초안). tipNo={}, taskId={}", saved.getNo(), taskId);
        return TipResponse.from(saved);
    }

    private String requestSummaryAsync(String url) {
        try {
            String endpoint = aiApiUrl + "/summary-index";
            Map<String, Object> body = Map.of("url", url);
            ResponseEntity<Map> resp = restTemplate.postForEntity(endpoint, body, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Object tid = resp.getBody().get("task_id");
                return tid != null ? String.valueOf(tid) : null;
            }
        } catch (RestClientException e) {
            log.warn("요약 비동기 요청 실패: {}", e.getMessage());
        }
        return null;
    }

    private List<Tag> upsertTags(Collection<String> names) {
        List<String> normalized = names.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();

        if (normalized.isEmpty()) return List.of();

        List<Tag> result = new ArrayList<>();
        for (String name : normalized) {
            Tag tag = tagRepository.findByName(name)
                    .orElseGet(() -> {
                        Tag t = new Tag();
                        t.setName(name);
                        return tagRepository.save(t);
                    });
            result.add(tag);
        }
        return result;
    }

    /* =========================================================
       2) 등록(보관함 연결 + 공개여부 반영) → 공개 시 알림 + 실시간 푸시
       ========================================================= */
    @Transactional
    public TipResponse registerTip(TipRegisterRequest request, Long loginUserNo) {
        if (request == null || request.getTipId() == null || request.getStorageId() == null) {
            throw new IllegalArgumentException("tipId, storageId는 필수입니다.");
        }

        Tip tip = tipRepository.findById(request.getTipId())
                .orElseThrow(() -> new IllegalArgumentException("팁을 찾을 수 없습니다."));

        if (!Objects.equals(tip.getUser().getNo(), loginUserNo)) {
            throw new AccessDeniedException("해당 팁에 대한 권한이 없습니다.");
        }

        Storage storage = storageRepository.findById(request.getStorageId())
                .orElseThrow(() -> new IllegalArgumentException("보관함을 찾을 수 없습니다."));

        if (storage.getUser() != null && !Objects.equals(storage.getUser().getNo(), loginUserNo)) {
            throw new AccessDeniedException("해당 보관함에 등록할 권한이 없습니다.");
        }

        if (!storageTipRepository.existsByStorageAndTip(storage, tip)) {
            StorageTip st = new StorageTip();
            st.setStorage(storage);
            st.setTip(tip);
            storageTipRepository.save(st);
        }

        if (request.getIsPublic() != null) {
            tip.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        }
        tip.setUpdatedAt(LocalDateTime.now());
        Tip saved = tipRepository.save(tip);

        // 공개글만 알림/푸시
        if (Boolean.TRUE.equals(saved.getIsPublic())) {
            notifyFollowers(saved);
            notifyGroupMembers(saved);
        }

        return TipResponse.from(saved);
    }

    /* =========================================================
       3) 수정/삭제
       ========================================================= */
    @Transactional
    public TipResponse update(Long tipNo, Long loginUserNo, TipUpdateRequest req) {
        Tip tip = tipRepository.findById(tipNo)
                .orElseThrow(() -> new IllegalArgumentException("팁을 찾을 수 없습니다."));
        if (!Objects.equals(tip.getUser().getNo(), loginUserNo)) {
            throw new AccessDeniedException("수정 권한이 없습니다.");
        }

        if (StringUtils.hasText(req.getTitle())) tip.setTitle(req.getTitle());
        if (req.getIsPublic() != null) tip.setIsPublic(req.getIsPublic());
        if (StringUtils.hasText(req.getContentSummary())) tip.setContentSummary(req.getContentSummary());
        tip.setUpdatedAt(LocalDateTime.now());

        Tip saved = tipRepository.save(tip);

        Map<String, Object> feed = Map.of(
                "type", "tip:update",
                "tipId", saved.getNo(),
                "author", Optional.ofNullable(saved.getUser().getNickname()).orElse(saved.getUser().getLoginId()),
                "tags", Optional.ofNullable(saved.getTipTags()).orElse(List.of()).stream()
                        .map(tt -> tt.getTag().getName()).toList(),
                "createdAt", saved.getCreatedAt(),
                "v", "v1"
        );
        messagingTemplate.convertAndSend("/topic/feed", feed);

        return TipResponse.from(saved);
    }

    @Transactional
    public void delete(Long tipNo, Long loginUserNo) {
        Tip tip = tipRepository.findById(tipNo)
                .orElseThrow(() -> new IllegalArgumentException("팁을 찾을 수 없습니다."));
        if (!Objects.equals(tip.getUser().getNo(), loginUserNo)) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }
        tipRepository.delete(tip);
    }

    /* =========================================================
       4) 자동 생성+등록 (AI 요약 폴링 포함) → 알림 + 실시간 푸시
       ========================================================= */
    @Transactional
    public TipResponse createAndRegisterTipAutoAsync(Long loginUserNo, TipAutoCreateRequest request) {
        User user = userRepository.findById(loginUserNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Storage storage = storageRepository.findById(request.getStorageNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 보관함입니다."));
        if (storage.getGroup() != null) {
            if (!groupMemberRepository.existsByGroupAndUser(storage.getGroup(), user)) {
                throw new AccessDeniedException("그룹 보관함에 등록하려면 해당 그룹의 멤버여야 합니다.");
            }
        } else if (!Objects.equals(storage.getUser().getNo(), loginUserNo)) {
            throw new AccessDeniedException("선택한 보관함은 현재 로그인한 사용자의 소유가 아닙니다.");
        }

        String taskId = requestSummaryAsync(request.getUrl());
        if (taskId == null) throw new RuntimeException("AI 요약 태스크 생성 실패");
        Map<String, Object> aiData = pollAiSummaryResult(taskId);

        Tip tip = Tip.builder()
                .user(user)
                .url(request.getUrl())
                .title((String) aiData.getOrDefault("title", "제목 없음"))
                .contentSummary((String) aiData.getOrDefault("summary", ""))
                .thumbnailUrl((String) aiData.get("thumbnailUrl"))
                .isPublic(true)
                .build();
        Tip savedTip = tipRepository.save(tip);

        List<String> tags = (List<String>) aiData.get("tags");
        if (tags != null && !tags.isEmpty()) {
            for (String tagName : new LinkedHashSet<>(tags)) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
                TipTag tipTag = TipTag.builder().tip(savedTip).tag(tag).build();
                tipTagRepository.save(tipTag);
                savedTip.getTipTags().add(tipTag);
            }
        }

        if (!storageTipRepository.existsByStorageAndTip(storage, savedTip)) {
            StorageTip st = StorageTip.builder().storage(storage).tip(savedTip).build();
            storageTipRepository.save(st);
            savedTip.getStorageTips().add(st);
        }

        // 자동 등록도 알림 + 실시간 푸시
        notifyFollowers(savedTip);
        notifyGroupMembers(savedTip);

        return TipResponse.from(savedTip);
    }

    private Map<String, Object> pollAiSummaryResult(String taskId) {
        String resultUrl = aiApiUrl + "/summary-result/" + taskId;
        int maxRetries = 30;
        long pollIntervalMs = 2000L;

        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(resultUrl, Map.class);
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();
                    Map<String, Object> result = new HashMap<>();
                    result.put("title", body.getOrDefault("title", "제목 없음"));
                    result.put("summary", body.getOrDefault("summary", "요약 정보 없음"));
                    result.put("tags", body.getOrDefault("tags", Collections.emptyList()));
                    result.put("thumbnailUrl", body.get("thumbnail_url"));
                    return result;
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() != HttpStatus.ACCEPTED) {
                    log.error("AI 결과 조회 실패(HTTP {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("AI 작업 결과를 가져오는 데 실패했습니다.");
                }
            } catch (Exception e) {
                log.error("AI 결과 조회 중 오류: {}", e.getMessage());
                throw new RuntimeException("AI 작업 결과 조회 오류");
            }

            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("AI 결과 대기 중 인터럽트");
            }
        }
        throw new RuntimeException("AI 작업 시간 초과: 요약 결과를 시간 내에 가져오지 못했습니다.");
    }

    /* =========================================================
       5) 알림(팔로워/그룹) + 개인 큐 푸시(Event)
       ========================================================= */
    private void notifyFollowers(Tip savedTip) {
        List<Follow> follows = followRepository.findByFollowing(savedTip.getUser());
        if (follows.isEmpty()) return;

        String actor = Optional.ofNullable(savedTip.getUser().getNickname())
                .orElse(savedTip.getUser().getLoginId());
        String title = StringUtils.hasText(savedTip.getTitle()) ? savedTip.getTitle() : "제목 없음";
        String message = actor + "님이 새 꿀팁을 등록했습니다: " + title;

        List<Notification> notis = follows.stream()
                .map(f -> Notification.builder()
                        .receiver(f.getFollower())
                        .tip(savedTip)
                        .type(NotificationType.FOLLOWING_TIP_UPLOAD)
                        .read(false)
                        .build())
                .toList();
        notificationRepository.saveAll(notis);

        // 실시간 개인 큐 푸시 (AFTER_COMMIT에서 전송)
        follows.forEach(f ->
                eventPublisher.publishEvent(
                        new NotificationCreatedEvent(
                                f.getFollower().getNo(),
                                savedTip.getNo(),
                                message,
                                Instant.now()
                        )
                )
        );
    }

    private void notifyGroupMembers(Tip savedTip) {
        Set<User> targets = savedTip.getStorageTips().stream()
                .map(StorageTip::getStorage)
                .filter(s -> s.getGroup() != null)
                .flatMap(s -> s.getGroup().getGroupMembers().stream())
                .map(GroupMember::getUser)
                .filter(u -> !u.getNo().equals(savedTip.getUser().getNo()))
                .collect(Collectors.toSet());
        if (targets.isEmpty()) return;

        String actor = Optional.ofNullable(savedTip.getUser().getNickname())
                .orElse(savedTip.getUser().getLoginId());
        String title = StringUtils.hasText(savedTip.getTitle()) ? savedTip.getTitle() : "제목 없음";
        String message = actor + "님이 그룹 보관함에 꿀팁을 등록했습니다: " + title;

        List<Notification> notis = targets.stream()
                .map(user -> Notification.builder()
                        .receiver(user)
                        .tip(savedTip)
                        .type(NotificationType.GROUP_TIP_UPLOAD)
                        .read(false)
                        .build())
                .toList();
        notificationRepository.saveAll(notis);

        // 실시간 개인 큐 푸시 (AFTER_COMMIT에서 전송)
        targets.forEach(u ->
                eventPublisher.publishEvent(
                        new NotificationCreatedEvent(
                                u.getNo(),
                                savedTip.getNo(),
                                message,
                                Instant.now()
                        )
                )
        );
    }
}
