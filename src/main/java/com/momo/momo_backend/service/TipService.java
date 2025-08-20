package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.*;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.enums.NotificationType;
import com.momo.momo_backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;

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
    private final GroupMemberRepository groupMemberRepository; // 그룹 멤버 리포지토리 추가

    // 꿀팁 생성 (AI 요약 요청 및 임시 팁 저장)
    @Transactional
    public TipResponse createTip(Long userNo, TipRequest request) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Tip tip = new Tip();
        tip.setUser(user);
        tip.setUrl(request.getUrl());
        tip.setTitle(request.getTitle()); // 사용자가 입력한 제목
        tip.setIsPublic(false); // 생성 단계에서는 기본적으로 비공개 (등록 단계에서 설정)
        tip.setCreatedAt(LocalDateTime.now());
        tip.setUpdatedAt(LocalDateTime.now());

        // AI 요약 요청 (FastAPI 서버 호출)
        RestTemplate restTemplate = new RestTemplate();
        String pythonUrl = "http://localhost:8000/async-index/"; // FastAPI 서버 URL
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> pythonRequest = Map.of("url", request.getUrl());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(pythonRequest, headers);

        try {
            ResponseEntity<Map> pythonResponse = restTemplate.postForEntity(pythonUrl, entity, Map.class);
            String taskId = (String) pythonResponse.getBody().get("task_id");
            tip.setContentSummary("요약 생성 중... (taskId: " + taskId + ")"); // 요약 대기 상태
        } catch (Exception e) {
            tip.setContentSummary("요약 실패: " + e.getMessage()); // 요약 실패 처리
        }

        Tip savedTip = tipRepository.save(tip); // 임시 팁 저장

        // 태그 처리 로직 (생성 단계에서 태그를 받았다면 저장)
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            Set<String> uniqueTagNames = new LinkedHashSet<>(request.getTags());

            for (String tagName : uniqueTagNames) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));

                TipTag tipTag = TipTag.builder()
                        .tip(savedTip)
                        .tag(tag)
                        .build();
                tipTagRepository.save(tipTag);
                savedTip.getTipTags().add(tipTag);
            }
        }

        return TipResponse.from(savedTip); // 생성된 팁의 기본 정보 반환
    }

    // 꿀팁 등록 (생성된 팁을 최종 보관함에 연결 및 공개 여부 설정)
    @Transactional
    public TipResponse registerTip(TipRegisterRequest request, Long userNo) {
        Tip tip = tipRepository.findById(request.getTipNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팁입니다."));

        // 팁 소유권 검증
        if (!tip.getUser().getNo().equals(userNo)) {
            throw new AccessDeniedException("해당 팁에 대한 등록 권한이 없습니다.");
        }

        // FastAPI 요약 결과 확인 및 업데이트 (재시도 로직 포함)
        if (tip.getContentSummary() != null && tip.getContentSummary().startsWith("요약 생성 중... (taskId:")) {
            String taskId = extractTaskId(tip.getContentSummary());
            if (taskId != null) {
                RestTemplate restTemplate = new RestTemplate();
                String summaryResultUrl = "http://localhost:8000/summary-result/" + taskId;
                int maxRetries = 5; // 최대 재시도 횟수
                long retryDelayMs = 2000; // 재시도 간 지연 시간 (2초)

                for (int i = 0; i < maxRetries; i++) {
                    try {
                        ResponseEntity<Map> summaryResponse = restTemplate.getForEntity(summaryResultUrl, Map.class);

                        // 요약, 제목, 태그, 썸네일 모두 가져옴
                        String finalSummary = (String) summaryResponse.getBody().get("summary");
                        String finalTitle = (String) summaryResponse.getBody().get("title");
                        List<String> finalTags = (List<String>) summaryResponse.getBody().get("tags");
                        String thumbnailData = (String) summaryResponse.getBody().get("thumbnail_data");
                        String thumbnailType = (String) summaryResponse.getBody().get("thumbnail_type");

                        if (finalSummary != null && !finalSummary.isEmpty()) {
                            tip.setContentSummary(finalSummary); // 최종 요약으로 업데이트

                            // 사용자가 제목을 입력하지 않았을 경우에만 AI 제목으로 업데이트
                            if (tip.getTitle() == null || tip.getTitle().isEmpty()) {
                                if (finalTitle != null && !finalTitle.isEmpty()) {
                                    tip.setTitle(finalTitle);
                                }
                            }

                            // 사용자가 태그를 입력하지 않았을 경우에만 AI 태그로 업데이트
                            if (tip.getTipTags().isEmpty()) { // 팁에 연결된 태그가 없는 경우
                                if (finalTags != null && !finalTags.isEmpty()) {
                                    // 기존 태그 삭제 후 새로운 태그 추가
                                    for (String tagName : finalTags) {
                                        Tag tag = tagRepository.findByName(tagName)
                                                .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
                                        TipTag tipTag = TipTag.builder().tip(tip).tag(tag).build();
                                        tipTagRepository.save(tipTag);
                                        tip.getTipTags().add(tipTag);
                                    }
                                }
                            }

                            // 썸네일 URL 업데이트
                            if (thumbnailData != null) {
                                if ("image".equals(thumbnailType)) {
                                    tip.setThumbnailUrl("data:image/png;base64," + thumbnailData);
                                } else if ("redirect".equals(thumbnailType)) {
                                    tip.setThumbnailUrl(thumbnailData);
                                }
                            }
                            break; // 성공했으므로 루프 종료
                        } else {
                            tip.setContentSummary("요약 내용이 아직 준비되지 않았거나 비어 있습니다.");
                            break;
                        }
                    } catch (HttpClientErrorException e) {
                        if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                            try {
                                Thread.sleep(retryDelayMs * (i + 1));
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                tip.setContentSummary("요약 조회 중단: " + ie.getMessage());
                                break;
                            }
                        } else if (e.getStatusCode() == HttpStatus.ACCEPTED) {
                            try {
                                Thread.sleep(retryDelayMs);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                tip.setContentSummary("요약 조회 중단: " + ie.getMessage());
                                break;
                            }
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
        }


        Storage storage = storageRepository.findById(request.getStorageNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 보관함입니다."));

        // 보관함 소유권 검증 (개인 보관함 또는 그룹 보관함 멤버 확인)
        if (storage.getGroup() != null) { // 그룹 보관함
            boolean isGroupMember = groupMemberRepository.existsByGroupAndUser(storage.getGroup(), tip.getUser());
            if (!isGroupMember) {
                throw new AccessDeniedException("그룹 보관함에 팁을 등록하려면 해당 그룹의 멤버여야 합니다.");
            }
        } else { // 개인 보관함
            if (!storage.getUser().getNo().equals(userNo)) {
                throw new AccessDeniedException("선택한 보관함은 현재 로그인한 사용자의 소유가 아닙니다.");
            }
        }

        // StorageTip 엔티티 생성 및 저장
        StorageTip storageTip = StorageTip.builder()
                .tip(tip)
                .storage(storage)
                .build();
        storageTipRepository.save(storageTip);

        // 꿀팁의 공개 여부 및 업데이트 시간 설정
        tip.setIsPublic(request.getIsPublic());
        tip.setUpdatedAt(LocalDateTime.now());
        Tip updatedTip = tipRepository.save(tip); // 팁 업데이트

        // 알림 전송 (팁이 최종 등록될 때)
        notifyFollowers(updatedTip);
        notifyGroupMembers(updatedTip);

        return TipResponse.from(updatedTip);
    }

    // taskId를 contentSummary에서 추출하는 헬퍼 메서드
    private String extractTaskId(String contentSummary) {
        int startIndex = contentSummary.indexOf("taskId:") + "taskId:".length();
        int endIndex = contentSummary.indexOf(")", startIndex);
        if (startIndex > -1 && endIndex > -1 && startIndex < endIndex) {
            return contentSummary.substring(startIndex, endIndex).trim();
        }
        return null;
    }


    // 팁 수정
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

    // 스케줄러에서 호출될, 오래된 미등록 꿀팁 삭제 메서드
    @Transactional
    public void deleteOldUnregisteredTips() {
        // 현재 시간으로부터 24시간 이전의 시간을 기준으로 설정
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        log.info("오래된 미등록 꿀팁 삭제 작업 시작. 기준 시간: {}", cutoffTime);

        List<Tip> oldUnregisteredTips = tipRepository.findUnregisteredTipsOlderThan(cutoffTime);

        if (oldUnregisteredTips.isEmpty()) {
            log.info("삭제할 오래된 미등록 꿀팁이 없습니다.");
            return;
        }

        // 삭제할 꿀팁들의 ID 로깅
        List<Long> tipNosToDelete = oldUnregisteredTips.stream()
                .map(Tip::getNo)
                .collect(Collectors.toList());
        log.info("삭제될 오래된 미등록 꿀팁 ID 목록: {}", tipNosToDelete);

        // 일괄 삭제 (TipTag 등 연관된 엔티티도 CascadeType.ALL에 의해 함께 삭제됨)
        tipRepository.deleteAll(oldUnregisteredTips);
        log.info("{}개의 오래된 미등록 꿀팁이 성공적으로 삭제되었습니다.", oldUnregisteredTips.size());
    }

    // 팁 삭제
    @Transactional
    public void delete(Long tipNo, Long userNo) {
        // 1. 삭제할 꿀팁을 찾고, 현재 로그인한 사용자가 소유자인지 확인합니다.
        Tip tip = tipRepository.findByNoAndUser_No(tipNo, userNo)
                .orElseThrow(() -> new AccessDeniedException("삭제 권한이 없습니다."));

        // 2. 꿀팁을 삭제합니다.
        //    Tip 엔티티의 @OneToMany에 설정된 cascade = CascadeType.ALL 옵션으로 인해
        //    연관된 모든 즐겨찾기(Bookmark), 보관함-꿀팁(StorageTip), 알림(Notification),
        //    꿀팁-태그(TipTag) 데이터가 자동으로 함께 삭제됩니다.
        tipRepository.delete(tip);
    }

    // 꿀팁 자동 생성 및 등록 (AI 요약 서버 호출)
    @Transactional
    public TipResponse createAndRegisterTipAutoAsync(Long userNo, TipAutoCreateRequest request) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Storage storage = storageRepository.findById(request.getStorageNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 보관함입니다."));

        // 보관함 소유권 또는 멤버십 검증
        if (storage.getGroup() != null) {
            if (!groupMemberRepository.existsByGroupAndUser(storage.getGroup(), user)) {
                throw new AccessDeniedException("그룹 보관함에 팁을 등록하려면 해당 그룹의 멤버여야 합니다.");
            }
        } else {
            if (!storage.getUser().getNo().equals(userNo)) {
                throw new AccessDeniedException("선택한 보관함은 현재 로그인한 사용자의 소유가 아닙니다.");
            }
        }

        // AI 요약 서버에 비동기 작업 요청
        String taskId = requestAiSummaryTask(request.getUrl());
        // AI 요약 결과 폴링
        Map<String, Object> aiData = pollAiSummaryResult(taskId);

        Tip tip = Tip.builder()
                .user(user)
                .url(request.getUrl())
                .title((String) aiData.get("title"))
                .contentSummary((String) aiData.get("summary"))
                .thumbnailUrl((String) aiData.get("thumbnailUrl"))
                .isPublic(true) // 항상 공개 상태로 저장
                .build(); // @Builder.Default로 초기화되므로 리스트 필드 명시적 초기화 불필요

        Tip savedTip = tipRepository.save(tip);

        // 태그 처리
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

        // 보관함에 바로 등록
        StorageTip storageTip = StorageTip.builder()
                .tip(savedTip)
                .storage(storage)
                .build();
        storageTipRepository.save(storageTip);
        savedTip.getStorageTips().add(storageTip); // 양방향 연관관계 설정

        // 알림 전송
        notifyFollowers(savedTip);
        notifyGroupMembers(savedTip);

        return TipResponse.from(savedTip);
    }

    // AI 서버에 비동기 작업을 요청하고 Task ID를 받는 메서드
    private String requestAiSummaryTask(String url) {
        RestTemplate restTemplate = new RestTemplate();
        String pythonApiUrl = "http://localhost:8000/async-index/";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = Map.of("url", url);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(pythonApiUrl, entity, Map.class);
            return (String) response.getBody().get("task_id");
        } catch (Exception e) {
            log.error("AI 작업 요청 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버에 작업을 요청하는 데 실패했습니다.");
        }
    }

    // Task ID로 AI 작업 결과를 폴링하는 메서드
    private Map<String, Object> pollAiSummaryResult(String taskId) {
        RestTemplate restTemplate = new RestTemplate();
        String resultUrl = "http://localhost:8000/summary-result/" + taskId;
        int maxRetries = 30; // 최대 30번 시도 (총 60초 대기)
        long pollIntervalMs = 2000; // 2초 간격으로 확인

        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(resultUrl, Map.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    Map<String, Object> body = response.getBody();
                    Map<String, Object> result = new HashMap<>();
                    result.put("title", body.getOrDefault("title", "제목 없음"));
                    result.put("summary", body.getOrDefault("summary", "요약 정보 없음"));
                    result.put("tags", body.getOrDefault("tags", Collections.emptyList()));

                    String thumbnailData = (String) body.get("thumbnail_data");
                    String thumbnailType = (String) body.get("thumbnail_type");
                    String thumbnailUrl = null;
                    if (thumbnailData != null) {
                        if ("image".equals(thumbnailType)) {
                            thumbnailUrl = "data:image/png;base64," + thumbnailData;
                        } else if ("redirect".equals(thumbnailType)) {
                            thumbnailUrl = thumbnailData;
                        }
                    }
                    result.put("thumbnailUrl", thumbnailUrl);
                    return result;
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() != HttpStatus.ACCEPTED) {
                    // 202 (Accepted) 외의 다른 클라이언트 오류는 실패로 간주
                    log.error("AI 결과 조회 실패 (HTTP {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("AI 작업 결과를 가져오는 데 실패했습니다.");
                }
                // 202 응답은 작업이 아직 진행 중이라는 의미이므로, 잠시 대기 후 계속
            } catch (Exception e) {
                log.error("AI 결과 조회 중 오류 발생: {}", e.getMessage());
                throw new RuntimeException("AI 작업 결과를 조회하는 중 오류가 발생했습니다.");
            }

            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("AI 결과 대기 중 스레드 오류가 발생했습니다.");
            }
        }
        throw new RuntimeException("AI 작업 시간 초과: 요약 결과를 시간 내에 가져올 수 없습니다.");
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

}
