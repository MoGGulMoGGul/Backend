package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.*;
import com.momo.momo_backend.dto.ai.AiResultResponseDto;
import com.momo.momo_backend.dto.ai.AiTaskResponseDto;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.enums.NotificationType;
import com.momo.momo_backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    // 1. 꿀팁 생성
    @Transactional
    public TipCreateResponse createTip(TipCreateRequest request) throws InterruptedException {
        // 1. AI 서버에 URL 분석 요청 보내고 Task ID 받기
        String processUrl = aiApiUrl + "/process";
        Map<String, String> body = new HashMap<>();
        body.put("url", request.getUrl());
        AiTaskResponseDto taskResponse = restTemplate.postForObject(processUrl, body, AiTaskResponseDto.class);
        String taskId = taskResponse.getTaskId();

        // 2. Polling으로 AI 분석 결과 받아오기
        String resultUrl = aiApiUrl + "/result/" + taskId;
        long startTime = System.currentTimeMillis();
        long timeout = 120000; // 2분 타임아웃

        while (System.currentTimeMillis() - startTime < timeout) {
            AiResultResponseDto resultResponse = restTemplate.getForObject(resultUrl, AiResultResponseDto.class);

            if (resultResponse != null && "SUCCESS".equals(resultResponse.getStatus())) {
                AiResultResponseDto.ResultData aiResult = resultResponse.getResult();

                // 3. 사용자 입력값과 AI 결과 조합
                String finalTitle = (request.getTitle() != null && !request.getTitle().isEmpty())
                        ? request.getTitle()
                        : aiResult.getTitle();

                List<String> finalTags = (request.getTags() != null && !request.getTags().isEmpty())
                        ? request.getTags()
                        : aiResult.getTags();

                // 4. 최종 결과 DTO 생성 및 반환
                return TipCreateResponse.builder()
                        .url(request.getUrl())
                        .title(finalTitle)
                        .tags(finalTags)
                        .summary(aiResult.getSummary())
                        .thumbnailImageUrl(aiResult.getThumbnailUrl())
                        .build();
            }

            // 2초 대기 후 다시 요청
            Thread.sleep(2000);
        }

        // 타임아웃 시 예외 발생
        throw new RuntimeException("AI processing timed out for task ID: " + taskId);
    }

    // 2. 꿀팁 등록: AI 작업 완료 여부를 '폴링'하여 확인 후 최종 등록
    @Transactional
    public TipRegisterResponse registerTip(TipRegisterRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Tip 엔티티 생성 시 contentSummary 필드 사용
        Tip tip = Tip.builder()
                .title(request.getTitle())
                .url(request.getUrl())
                .contentSummary(request.getSummary()) // 'summary' 대신 'contentSummary'
                .thumbnailUrl(request.getThumbnailImageUrl())
                .isPublic(request.getIsPublic())
                .user(user)
                .build();
        tipRepository.save(tip);

        List<String> tagNames = request.getTags() != null ? request.getTags() : Collections.emptyList();
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName)));
            // TipTag 생성자 대신 빌더 사용
            tipTagRepository.save(TipTag.builder().tip(tip).tag(tag).build());
        }

        Storage storage = storageRepository.findById(request.getStorageNo())
                .orElseThrow(() -> new RuntimeException("Storage not found with id: " + request.getStorageNo()));

        // createStorageTip 대신 save 메서드 사용
        StorageTip storageTip = StorageTip.builder()
                .storage(storage)
                .tip(tip)
                .build();
        storageTipRepository.save(storageTip);

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

    // Task ID로 AI 작업 결과를 폴링하는 메서드
    private Map<String, Object> pollAiSummaryResult(String taskId) {
        RestTemplate restTemplate = new RestTemplate();
        String resultUrl = aiApiUrl + "/summary-result/" + taskId;
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

                    // 이제 thumbnailUrl을 직접 받아서 사용합니다.
                    result.put("thumbnailUrl", body.get("thumbnail_url"));

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