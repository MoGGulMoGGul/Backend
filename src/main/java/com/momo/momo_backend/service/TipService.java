// src/main/java/com/momo/momo_backend/service/TipService.java
package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.*;
import com.momo.momo_backend.dto.ai.AiResultResponseDto;
import com.momo.momo_backend.dto.ai.AiTaskResponseDto;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.enums.NotificationType;
import com.momo.momo_backend.realtime.events.NotificationCreatedEvent;
import com.momo.momo_backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipService {

    @Value("${ai.server.url}")
    private String aiApiUrl;

    private final TipRepository tipRepository;
    private final TagRepository tagRepository;
    private final TipTagRepository tipTagRepository;
    private final UserRepository userRepository;
    private final StorageRepository storageRepository;
    private final FollowRepository followRepository;
    private final NotificationRepository notificationRepository;
    private final StorageTipRepository storageTipRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final RestTemplate restTemplate;

    // 공개 피드 브로드캐스트 & 개인 큐 이벤트
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    /* 1) 꿀팁 생성(미리보기): AI 폴링만 → DB 저장/브로드캐스트 안 함 */
    @Transactional
    public TipCreateResponse createTip(TipCreateRequest request) throws InterruptedException {
        String processUrl = aiApiUrl + "/async-index/";
        Map<String, String> body = new HashMap<>();
        body.put("url", request.getUrl());

        AiTaskResponseDto task = restTemplate.postForObject(processUrl, body, AiTaskResponseDto.class);
        String taskId = (task != null) ? task.getTaskId() : null;
        if (!StringUtils.hasText(taskId)) throw new RuntimeException("AI task 생성 실패");

        String resultUrl = aiApiUrl + "/task-status/" + taskId;
        long start = System.currentTimeMillis();
        long timeoutMs = 120_000;

        while (System.currentTimeMillis() - start < timeoutMs) {
            AiResultResponseDto result = restTemplate.getForObject(resultUrl, AiResultResponseDto.class);
            if (result != null && "SUCCESS".equals(result.getStatus())) {
                AiResultResponseDto.ResultData r = result.getResult();
                String finalTitle = StringUtils.hasText(request.getTitle()) ? request.getTitle() : r.getTitle();
                List<String> finalTags = (request.getTags() != null && !request.getTags().isEmpty())
                        ? request.getTags() : r.getTags();
                return TipCreateResponse.builder()
                        .url(request.getUrl())
                        .title(finalTitle)
                        .tags(finalTags)
                        .summary(r.getSummary())
                        .thumbnailImageUrl(r.getThumbnailUrl())
                        .build();
            }
            Thread.sleep(2000);
        }
        throw new RuntimeException("AI processing timed out: " + taskId);
    }

    /* 2) 최종 등록: 저장 + 태그 upsert + 보관함 매핑 + 피드 + 개인 알림 */
    @Transactional
    public TipRegisterResponse registerTip(TipRegisterRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Tip tip = Tip.builder()
                .title(StringUtils.hasText(request.getTitle()) ? request.getTitle() : "제목 없음")
                .url(request.getUrl())
                .contentSummary(request.getSummary())
                .thumbnailUrl(request.getThumbnailImageUrl())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .user(user)
                .build();
        tip.setCreatedAt(LocalDateTime.now());
        tip.setUpdatedAt(LocalDateTime.now());
        tipRepository.save(tip);

        // 태그 upsert + 매핑
        List<String> tagNames = (request.getTags() != null) ? request.getTags() : Collections.emptyList();
        for (String tagName : new LinkedHashSet<>(tagNames)) {
            if (!StringUtils.hasText(tagName)) continue;
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
            TipTag link = TipTag.builder().tip(tip).tag(tag).build();
            tipTagRepository.save(link);
            tip.getTipTags().add(link); // 브로드캐스트용 태그 최신화
        }

        // 보관함 매핑 (중복 방지 + 그룹 보관함 권한 체크)
        Storage storage = storageRepository.findById(request.getStorageNo())
                .orElseThrow(() -> new RuntimeException("Storage not found with id: " + request.getStorageNo()));
        if (storage.getGroup() != null) {
            if (!groupMemberRepository.existsByGroupAndUser(storage.getGroup(), user)) {
                throw new AccessDeniedException("그룹 보관함에 등록하려면 해당 그룹의 멤버여야 합니다.");
            }
        } else if (!Objects.equals(storage.getUser().getNo(), userId)) {
            throw new AccessDeniedException("선택한 보관함은 현재 로그인한 사용자의 소유가 아닙니다.");
        }

        if (!storageTipRepository.existsByStorageAndTip(storage, tip)) {
            StorageTip storageTip = StorageTip.builder()
                    .storage(storage)
                    .tip(tip)
                    .build();
            storageTipRepository.save(storageTip);
            tip.getStorageTips().add(storageTip); // 메모리 컬렉션도 동기화(그룹 알림 누락 방지)
        }

        // 공개 피드 브로드캐스트
        Map<String, Object> feed = Map.of(
                "type", "tip:new",
                "tipId", tip.getNo(),
                "author", Optional.ofNullable(user.getNickname()).orElse(user.getLoginId()),
                "tags", tip.getTipTags().stream().map(tt -> tt.getTag().getName()).toList(),
                "createdAt", tip.getCreatedAt(),
                "v", "v1"
        );
        messagingTemplate.convertAndSend("/topic/feed", feed);

        // 공개글이면 알림 저장 + 개인 큐 이벤트
        if (Boolean.TRUE.equals(tip.getIsPublic())) {
            notifyFollowers(tip);
            notifyGroupMembers(tip);
        }

        return TipRegisterResponse.builder()
                .tipNo(tip.getNo())
                .storageNo(storage.getNo())
                .isPublic(tip.getIsPublic())
                .summary(tip.getContentSummary())
                .url(tip.getUrl())
                .userNo(user.getNo())
                .nickname(user.getNickname())
                .thumbnailImageUrl(tip.getThumbnailUrl())
                .title(tip.getTitle())
                .tags(tagNames)
                .build();
    }

    /* 3) 수정/삭제 (수정 시 피드 업데이트 방송 유지) */
    @Transactional
    public TipResponse update(Long tipNo, Long userNo, TipUpdateRequest req) {
        Tip tip = tipRepository.findByNoAndUser_No(tipNo, userNo)
                .orElseThrow(() -> new AccessDeniedException("수정 권한이 없습니다."));

        if (req.getTitle() != null) tip.setTitle(req.getTitle());
        if (req.getContentSummary() != null) tip.setContentSummary(req.getContentSummary());
        if (req.getIsPublic() != null) tip.setIsPublic(req.getIsPublic());

        if (req.getTags() != null) {
            tipTagRepository.deleteByTipNo(tipNo);
            tip.getTipTags().clear();
            for (String name : new LinkedHashSet<>(req.getTags())) {
                if (!StringUtils.hasText(name)) continue;
                Tag tag = tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
                tip.getTipTags().add(TipTag.builder().tip(tip).tag(tag).build());
            }
        }

        Tip saved = tipRepository.save(tip);

        Map<String, Object> feed = Map.of(
                "type", "tip:update",
                "tipId", saved.getNo(),
                "author", Optional.ofNullable(saved.getUser().getNickname()).orElse(saved.getUser().getLoginId()),
                "tags", saved.getTipTags().stream().map(tt -> tt.getTag().getName()).toList(),
                "createdAt", saved.getCreatedAt(),
                "v", "v1"
        );
        messagingTemplate.convertAndSend("/topic/feed", feed);

        return TipResponse.from(saved);
    }

    @Transactional
    public void delete(Long tipNo, Long userNo) {
        Tip tip = tipRepository.findByNoAndUser_No(tipNo, userNo)
                .orElseThrow(() -> new AccessDeniedException("삭제 권한이 없습니다."));
        tipRepository.delete(tip);
    }

    /* 4) 자동 생성+등록 (AI 폴링 포함) → 저장 + 태그 + 보관함 + 피드 + 개인 알림 */
    @Transactional
    public TipResponse createAndRegisterTipAutoAsync(Long userNo, TipAutoCreateRequest request) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Storage storage = storageRepository.findById(request.getStorageNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 보관함입니다."));
        if (storage.getGroup() != null) {
            if (!groupMemberRepository.existsByGroupAndUser(storage.getGroup(), user)) {
                throw new AccessDeniedException("그룹 보관함에 등록하려면 해당 그룹의 멤버여야 합니다.");
            }
        } else if (!Objects.equals(storage.getUser().getNo(), userNo)) {
            throw new AccessDeniedException("선택한 보관함은 현재 로그인한 사용자의 소유가 아닙니다.");
        }

        String taskId = requestSummaryAsync(request.getUrl());
        if (taskId == null) throw new RuntimeException("AI 요약 태스크 생성 실패");
        Map<String, Object> ai = pollAiSummaryResult(taskId);

        Tip tip = Tip.builder()
                .user(user)
                .url(request.getUrl())
                .title((String) ai.getOrDefault("title", "제목 없음"))
                .contentSummary((String) ai.getOrDefault("summary", ""))
                .thumbnailUrl((String) ai.get("thumbnailUrl"))
                .isPublic(true)
                .build();
        tip.setCreatedAt(LocalDateTime.now());
        tip.setUpdatedAt(LocalDateTime.now());
        Tip savedTip = tipRepository.save(tip);

        List<String> tags = (List<String>) ai.getOrDefault("tags", List.of());
        for (String tagName : new LinkedHashSet<>(tags)) {
            if (!StringUtils.hasText(tagName)) continue;
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
            TipTag tipTag = TipTag.builder().tip(savedTip).tag(tag).build();
            tipTagRepository.save(tipTag);
            savedTip.getTipTags().add(tipTag);
        }

        if (!storageTipRepository.existsByStorageAndTip(storage, savedTip)) {
            StorageTip storageTip = StorageTip.builder()
                    .storage(storage)
                    .tip(savedTip)
                    .build();
            storageTipRepository.save(storageTip);
            savedTip.getStorageTips().add(storageTip); // 메모리 컬렉션 동기화(그룹 알림 누락 방지)
        }

        Map<String, Object> feed = Map.of(
                "type", "tip:new",
                "tipId", savedTip.getNo(),
                "author", Optional.ofNullable(user.getNickname()).orElse(user.getLoginId()),
                "tags", savedTip.getTipTags().stream().map(tt -> tt.getTag().getName()).toList(),
                "createdAt", savedTip.getCreatedAt(),
                "v", "v1"
        );
        messagingTemplate.convertAndSend("/topic/feed", feed);

        notifyFollowers(savedTip);
        notifyGroupMembers(savedTip);

        return TipResponse.from(savedTip);
    }

    /* ====== AI 요청/폴링 ====== */
    private String requestSummaryAsync(String url) {
        try {
            String endpoint = aiApiUrl + "/summary-index";
            Map<String, Object> body = Map.of("url", url);
            ResponseEntity<Map> resp = restTemplate.postForEntity(endpoint, body, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Object tid = resp.getBody().get("task_id");
                return (tid != null) ? String.valueOf(tid) : null;
            }
        } catch (RestClientException e) {
            log.warn("요약 비동기 요청 실패: {}", e.getMessage());
        }
        return null;
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

            try { Thread.sleep(pollIntervalMs); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new RuntimeException("AI 결과 대기 중 인터럽트"); }
        }
        throw new RuntimeException("AI 작업 시간 초과: 요약 결과를 시간 내에 가져오지 못했습니다.");
    }

    /* ====== 알림 저장 + 개인 큐 이벤트 발행 ====== */
    private void notifyFollowers(Tip savedTip) {
        List<Follow> follows = followRepository.findByFollowing(savedTip.getUser());
        if (follows.isEmpty()) return;

        String actor = Optional.ofNullable(savedTip.getUser().getNickname()).orElse(savedTip.getUser().getLoginId());
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
        if (!notis.isEmpty()) notificationRepository.saveAll(notis);

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

        String actor = Optional.ofNullable(savedTip.getUser().getNickname()).orElse(savedTip.getUser().getLoginId());
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
        if (!notis.isEmpty()) notificationRepository.saveAll(notis);

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
