package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.*;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.enums.NotificationType;
import com.momo.momo_backend.repository.*;
import com.momo.momo_backend.realtime.events.NotificationCreatedEvent;
import com.momo.momo_backend.realtime.events.TipCreatedEvent;
import com.momo.momo_backend.realtime.events.TipUpdatedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipService {

    private final TipRepository tipRepository;
    private final TagRepository tagRepository;
    private final TipTagRepository tipTagRepository;
    private final UserRepository userRepository;
    private final StorageRepository storageRepository;
    private final FollowRepository followRepository;
    private final NotificationRepository notificationRepository;
    private final StorageTipRepository storageTipRepository;
    private final GroupMemberRepository groupMemberRepository;

    /** 도메인 이벤트 발행 */
    private final ApplicationEventPublisher events;

    // =========================================================
    // 1) 꿀팁 생성 (AI 요약 비동기 요청 + 임시 저장)
    // =========================================================
    @Transactional
    public TipResponse createTip(Long userNo, TipRequest request) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Tip tip = new Tip();
        tip.setUser(user);
        tip.setUrl(request.getUrl());
        tip.setTitle(request.getTitle());     // 사용자가 입력한 제목(없으면 null)
        tip.setIsPublic(false);               // 생성 단계: 비공개
        tip.setCreatedAt(LocalDateTime.now());
        tip.setUpdatedAt(LocalDateTime.now());

        // FastAPI에 요약 비동기 요청
        RestTemplate restTemplate = new RestTemplate();
        String pythonUrl = "http://localhost:8000/async-index/";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> pythonRequest = Map.of("url", request.getUrl());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(pythonRequest, headers);

        try {
            ResponseEntity<Map> pythonResponse = restTemplate.postForEntity(pythonUrl, entity, Map.class);
            String taskId = (String) pythonResponse.getBody().get("task_id");
            tip.setContentSummary("요약 생성 중... (taskId: " + taskId + ")");
        } catch (Exception e) {
            log.warn("AI 요약 비동기 요청 실패: {}", e.getMessage());
            tip.setContentSummary("요약 실패: " + e.getMessage());
        }

        Tip savedTip = tipRepository.save(tip);

        // 태그 선반영(사용자가 입력한 경우)
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tagName : new LinkedHashSet<>(request.getTags())) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
                TipTag tipTag = TipTag.builder().tip(savedTip).tag(tag).build();
                tipTagRepository.save(tipTag);
                savedTip.getTipTags().add(tipTag);
            }
        }

        return TipResponse.from(savedTip);
    }

    // =========================================================
    // 2) 꿀팁 등록 (보관함 연결 + 공개여부 설정 + 실시간 이벤트)
    //    - 등록 시점에 FastAPI 요약 결과를 가져와 반영 시도
    // =========================================================
    @Transactional
    public TipResponse registerTip(TipRegisterRequest request, Long userNo) {
        Tip tip = tipRepository.findById(request.getTipId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팁입니다."));

        // 소유자 검증
        if (!tip.getUser().getNo().equals(userNo)) {
            throw new AccessDeniedException("해당 팁에 대한 등록 권한이 없습니다.");
        }

        // FastAPI 요약 결과 반영(필요 시)
        if (tip.getContentSummary() != null && tip.getContentSummary().startsWith("요약 생성 중... (taskId:")) {
            String taskId = extractTaskId(tip.getContentSummary());
            if (taskId != null) {
                applyAiSummaryResultIfReady(tip, taskId);
            }
        }

        // 보관함 검증
        Storage storage = storageRepository.findById(request.getStorageId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 보관함입니다."));

        if (storage.getGroup() != null) { // 그룹 보관함
            boolean isGroupMember = groupMemberRepository.existsByGroupAndUser(storage.getGroup(), tip.getUser());
            if (!isGroupMember) throw new AccessDeniedException("그룹 보관함에 팁을 등록하려면 해당 그룹의 멤버여야 합니다.");
        } else { // 개인 보관함
            if (!storage.getUser().getNo().equals(userNo)) {
                throw new AccessDeniedException("선택한 보관함은 현재 로그인한 사용자의 소유가 아닙니다.");
            }
        }

        // 보관함 연결
        StorageTip storageTip = StorageTip.builder().tip(tip).storage(storage).build();
        storageTipRepository.save(storageTip);

        // 공개여부/갱신시간 반영
        tip.setIsPublic(request.getIsPublic());
        tip.setUpdatedAt(LocalDateTime.now());
        Tip updatedTip = tipRepository.save(tip);

        // 실시간: 등록 직후 NEW + (가능하면) UPDATE 업서트
        events.publishEvent(new TipCreatedEvent(updatedTip.getNo()));
        events.publishEvent(new TipUpdatedEvent(updatedTip.getNo()));

        // 알림
        notifyFollowers(updatedTip);
        notifyGroupMembers(updatedTip);

        return TipResponse.from(updatedTip);
    }

    // FastAPI 결과 반영 유틸
    private void applyAiSummaryResultIfReady(Tip tip, String taskId) {
        RestTemplate restTemplate = new RestTemplate();
        String summaryResultUrl = "http://localhost:8000/summary-result/" + taskId;
        int maxRetries = 5;
        long retryDelayMs = 2000;

        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<Map> resp = restTemplate.getForEntity(summaryResultUrl, Map.class);
                String finalSummary = (String) resp.getBody().get("summary");
                String finalTitle   = (String) resp.getBody().get("title");
                List<String> finalTags = (List<String>) resp.getBody().get("tags");
                String thumbnailData = (String) resp.getBody().get("thumbnail_data");
                String thumbnailType = (String) resp.getBody().get("thumbnail_type");

                if (finalSummary != null && !finalSummary.isEmpty()) {
                    tip.setContentSummary(finalSummary);

                    // 제목 비어있으면 AI 제목 채움
                    if (tip.getTitle() == null || tip.getTitle().isEmpty()) {
                        if (finalTitle != null && !finalTitle.isEmpty()) tip.setTitle(finalTitle);
                    }
                    // 태그가 없으면 AI 태그 채움
                    if (tip.getTipTags().isEmpty() && finalTags != null && !finalTags.isEmpty()) {
                        for (String tagName : finalTags) {
                            Tag tag = tagRepository.findByName(tagName)
                                    .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
                            TipTag tipTag = TipTag.builder().tip(tip).tag(tag).build();
                            tipTagRepository.save(tipTag);
                            tip.getTipTags().add(tipTag);
                        }
                    }
                    // 썸네일
                    if (thumbnailData != null) {
                        if ("image".equals(thumbnailType)) tip.setThumbnailUrl("data:image/png;base64," + thumbnailData);
                        else if ("redirect".equals(thumbnailType)) tip.setThumbnailUrl(thumbnailData);
                    }
                    break;
                } else {
                    tip.setContentSummary("요약 내용이 아직 준비되지 않았거나 비어 있습니다.");
                    break;
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    sleepQuiet(retryDelayMs * (i + 1));
                } else if (e.getStatusCode() == HttpStatus.ACCEPTED) {
                    sleepQuiet(retryDelayMs);
                } else {
                    tip.setContentSummary("요약 조회 실패: " + e.getMessage());
                    break;
                }
            } catch (Exception e) {
                tip.setContentSummary("요약 조회 실패: " + e.getMessage());
                break;
            }
        }
    }

    private static void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    // =========================================================
    // 3) 팁 수정 (업데이트 이벤트)
    // =========================================================
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
                Tag tag = tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
                TipTag tt = TipTag.builder().tip(tip).tag(tag).build();
                tip.getTipTags().add(tt);
            }
        }

        // 실시간: 업데이트 이벤트 (커밋 후 송신)
        events.publishEvent(new TipUpdatedEvent(tipNo));

        return TipResponse.from(tip);
    }

    // =========================================================
    // 4) 오래된 미등록 팁 삭제(스케줄러용)
    // =========================================================
    @Transactional
    public void deleteOldUnregisteredTips() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        log.info("오래된 미등록 꿀팁 삭제 시작. 기준: {}", cutoffTime);

        List<Tip> olds = tipRepository.findUnregisteredTipsOlderThan(cutoffTime);
        if (olds.isEmpty()) {
            log.info("삭제 대상 없음");
            return;
        }
        List<Long> ids = olds.stream().map(Tip::getNo).collect(Collectors.toList());
        log.info("삭제될 꿀팁 ID: {}", ids);

        tipRepository.deleteAll(olds);
        log.info("{}개의 오래된 미등록 꿀팁 삭제 완료", olds.size());
    }

    // =========================================================
    // 5) 팁 삭제
    // =========================================================
    @Transactional
    public void delete(Long tipNo, Long userNo) {
        Tip tip = tipRepository.findByNoAndUser_No(tipNo, userNo)
                .orElseThrow(() -> new AccessDeniedException("삭제 권한이 없습니다."));
        tipRepository.delete(tip); // 연관 Cascade 설정 가정
    }

    // =========================================================
    // 6) 태그 기반 조회
    // =========================================================
    public List<TipResponse> getTipsByTag(String tagName) {
        return tipRepository.findTipsByTagName(tagName)
                .stream().map(TipResponse::from).toList();
    }

    // =========================================================
    // 7) 직접 생성 저장(필요 시 사용)
    // =========================================================
    public Tip createTip(Tip tip) {
        Tip savedTip = tipRepository.save(tip);
        notifyFollowers(savedTip);
        notifyGroupMembers(savedTip);
        return savedTip;
    }

    // =========================================================
    // 8) 자동 생성+등록 (AI 완료까지 폴링) — 선택 사용
    // =========================================================
    @Transactional
    public TipResponse createAndRegisterTipAutoAsync(Long userNo, TipAutoCreateRequest request) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        Storage storage = storageRepository.findById(request.getStorageId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 보관함입니다."));

        // 보관함 권한
        if (storage.getGroup() != null) {
            if (!groupMemberRepository.existsByGroupAndUser(storage.getGroup(), user)) {
                throw new AccessDeniedException("그룹 보관함에 팁을 등록하려면 해당 그룹의 멤버여야 합니다.");
            }
        } else {
            if (!storage.getUser().getNo().equals(userNo)) {
                throw new AccessDeniedException("선택한 보관함은 현재 로그인한 사용자의 소유가 아닙니다.");
            }
        }

        // AI 작업 요청 + 결과 폴링
        String taskId = requestAiSummaryTask(request.getUrl());
        Map<String, Object> ai = pollAiSummaryResult(taskId);

        Tip tip = Tip.builder()
                .user(user)
                .url(request.getUrl())
                .title((String) ai.get("title"))
                .contentSummary((String) ai.get("summary"))
                .thumbnailUrl((String) ai.get("thumbnailUrl"))
                .isPublic(true)
                .build();

        Tip savedTip = tipRepository.save(tip);

        // 태그
        List<String> tags = (List<String>) ai.get("tags");
        if (tags != null && !tags.isEmpty()) {
            for (String tagName : new LinkedHashSet<>(tags)) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
                TipTag tt = TipTag.builder().tip(savedTip).tag(tag).build();
                tipTagRepository.save(tt);
                savedTip.getTipTags().add(tt);
            }
        }

        // 보관함 등록
        StorageTip storageTip = StorageTip.builder().tip(savedTip).storage(storage).build();
        storageTipRepository.save(storageTip);
        savedTip.getStorageTips().add(storageTip);

        // 실시간 이벤트 (등록 + 업데이트)
        events.publishEvent(new TipCreatedEvent(savedTip.getNo()));
        events.publishEvent(new TipUpdatedEvent(savedTip.getNo()));

        // 알림
        notifyFollowers(savedTip);
        notifyGroupMembers(savedTip);

        return TipResponse.from(savedTip);
    }

    // --- AI 비동기 요약 요청/폴링 유틸 ---------------------------------

    private String requestAiSummaryTask(String url) {
        RestTemplate restTemplate = new RestTemplate();
        String pythonApiUrl = "http://localhost:8000/async-index/";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("url", url);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(pythonApiUrl, entity, Map.class);
            return (String) response.getBody().get("task_id");
        } catch (Exception e) {
            log.error("AI 작업 요청 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버 작업 요청 실패");
        }
    }

    private Map<String, Object> pollAiSummaryResult(String taskId) {
        RestTemplate restTemplate = new RestTemplate();
        String resultUrl = "http://localhost:8000/summary-result/" + taskId;
        int maxRetries = 30;         // 60초
        long pollIntervalMs = 2000;  // 2초

        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(resultUrl, Map.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    Map<String, Object> body = response.getBody();
                    Map<String, Object> result = new HashMap<>();
                    result.put("title",    body.getOrDefault("title", "제목 없음"));
                    result.put("summary",  body.getOrDefault("summary", "요약 정보 없음"));
                    result.put("tags",     body.getOrDefault("tags", Collections.emptyList()));

                    String thumbnailData = (String) body.get("thumbnail_data");
                    String thumbnailType = (String) body.get("thumbnail_type");
                    String thumbnailUrl = null;
                    if (thumbnailData != null) {
                        if ("image".equals(thumbnailType)) thumbnailUrl = "data:image/png;base64," + thumbnailData;
                        else if ("redirect".equals(thumbnailType)) thumbnailUrl = thumbnailData;
                    }
                    result.put("thumbnailUrl", thumbnailUrl);
                    return result;
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() != HttpStatus.ACCEPTED) {
                    log.error("AI 결과 조회 실패 (HTTP {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("AI 작업 결과 조회 실패");
                }
                // 202: 진행중 → 대기 후 재시도
            } catch (Exception e) {
                log.error("AI 결과 조회 중 오류: {}", e.getMessage());
                throw new RuntimeException("AI 작업 결과 조회 중 오류");
            }

            sleepQuiet(pollIntervalMs);
        }
        throw new RuntimeException("AI 작업 시간초과: 결과를 가져오지 못했습니다.");
    }

    // --- 공용 유틸 ------------------------------------------------------

    /** contentSummary에서 taskId 추출 */
    private String extractTaskId(String contentSummary) {
        int idx = contentSummary.indexOf("taskId:");
        if (idx < 0) return null;
        int start = idx + "taskId:".length();
        int end = contentSummary.indexOf(")", start);
        if (end > start) return contentSummary.substring(start, end).trim();
        return null;
    }

    // --- 알림 전송(저장 + 실시간 이벤트 발행) ---------------------------

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

        if (notis.isEmpty()) return;

        List<Notification> saved = notificationRepository.saveAll(notis);

        String msg = (savedTip.getUser().getNickname() != null
                ? savedTip.getUser().getNickname() : "팔로잉") + "님이 새 꿀팁을 등록했습니다.";

        for (Notification n : saved) {
            events.publishEvent(new NotificationCreatedEvent(
                    n.getReceiver().getNo(),
                    savedTip.getNo(),
                    msg,
                    Instant.now()
            ));
        }
    }

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

        if (notis.isEmpty()) return;

        List<Notification> saved = notificationRepository.saveAll(notis);

        String msg = "그룹 보관함에 새 꿀팁이 등록됐어요: " +
                Optional.ofNullable(savedTip.getTitle()).orElse("제목 없음");

        for (Notification n : saved) {
            events.publishEvent(new NotificationCreatedEvent(
                    n.getReceiver().getNo(),
                    savedTip.getNo(),
                    msg,
                    Instant.now()
            ));
        }
    }
}
