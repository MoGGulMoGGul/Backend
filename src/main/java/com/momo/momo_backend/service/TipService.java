package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.TipAutoCreateRequest;
import com.momo.momo_backend.dto.TipRegisterRequest;
import com.momo.momo_backend.dto.TipRequest;
import com.momo.momo_backend.dto.TipResponse;
import com.momo.momo_backend.dto.TipUpdateRequest;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.realtime.service.TipViewsRankService;
import com.momo.momo_backend.repository.BookmarkRepository;
import com.momo.momo_backend.repository.StorageRepository;
import com.momo.momo_backend.repository.StorageTipRepository;
import com.momo.momo_backend.repository.TagRepository;
import com.momo.momo_backend.repository.TipRepository;
import com.momo.momo_backend.repository.TipTagRepository;
import com.momo.momo_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipService {

    private final TipRepository tipRepository;
    private final UserRepository userRepository;
    private final StorageRepository storageRepository;
    private final StorageTipRepository storageTipRepository;
    private final TagRepository tagRepository;
    private final TipTagRepository tipTagRepository;
    private final BookmarkRepository bookmarkRepository;

    private final TipViewsRankService tipViewsRankService;   // 조회수 랭킹 기록용(컨트롤러에서 주로 사용)
    private final SimpMessagingTemplate messagingTemplate;   // 피드 브로드캐스트

    /* =========================================================
       1) 생성 (URL 수집/요약 큐 태우기)
       ========================================================= */

    /** TipController에서 부르는 기본 진입점 */
    @Transactional
    public TipResponse createTip(Long loginUserNo, TipRequest request) {
        return generate(loginUserNo, request);
    }

    /** 실제 생성 로직 */
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
        tip.setIsPublic(false); // ✅ Tip 엔티티는 보통 isPublic 필드 → setIsPublic 사용
        tip.setCreatedAt(LocalDateTime.now());
        tip.setUpdatedAt(LocalDateTime.now());

        // 1) AI 요약 비동기 태우기 (FastAPI) → taskId 확보
        String taskId = requestSummaryAsync(request.getUrl());

        // 2) contentSummary에 태스크 정보를 넣어두기 (Postman에서 정규식으로 추출)
        if (taskId != null) {
            tip.setContentSummary("요약 생성 중... (taskId: " + taskId + ")");
        } else {
            tip.setContentSummary("요약 대기 중...");
        }

        // 3) 초기 태그(선택)
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

        // (선택) 피드 브로드캐스트: 최소 페이로드 tip:new
        Map<String, Object> feed = Map.of(
                "type", "tip:new",
                "tipId", saved.getNo(),
                "author", author.getNickname() != null ? author.getNickname() : author.getLoginId(),
                "tags", Optional.ofNullable(saved.getTipTags()).orElse(List.of())
                        .stream().map(tt -> tt.getTag().getName()).toList(),
                "createdAt", saved.getCreatedAt(),
                "v", "v1"
        );
        messagingTemplate.convertAndSend("/topic/feed", feed);

        log.info("꿀팁 생성 완료 (초안). tipNo={}", saved.getNo());
        return TipResponse.from(saved);
    }

    /**
     * FastAPI(8000)로 비동기 요약 작업을 요청하고 taskId를 받는다.
     * 실패 시 null 반환(서버는 이후 동기화/재시도 로직으로 커버).
     */
    private String requestSummaryAsync(String url) {
        String endpoint = "http://localhost:8000/summary-index"; // 필요시 properties로 분리
        RestTemplate rt = new RestTemplate();
        Map<String, Object> body = Map.of("url", url);

        try {
            var resp = rt.postForEntity(endpoint, body, Map.class);
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

        List<Tag> existing = tagRepository.findByNameIn(normalized);
        Set<String> existNames = existing.stream().map(Tag::getName).collect(Collectors.toSet());

        List<Tag> toCreate = normalized.stream()
                .filter(n -> !existNames.contains(n))
                .map(n -> {
                    Tag t = new Tag();
                    t.setName(n);
                    return t;
                })
                .toList();

        if (!toCreate.isEmpty()) {
            tagRepository.saveAll(toCreate);
        }

        if (toCreate.isEmpty()) return existing;
        List<Tag> all = new ArrayList<>(existing);
        all.addAll(toCreate);
        return all;
    }

    /* =========================================================
       2) 등록(보관함 담기 + 공개여부 반영)
       ========================================================= */
    @Transactional
    public TipResponse registerTip(TipRegisterRequest request, Long loginUserNo) {
        // ✅ 현재 DTO는 tipId/storageId 필드가 표준
        if (request == null || request.getTipId() == null || request.getStorageId() == null) {
            throw new IllegalArgumentException("tipId, storageId는 필수입니다.");
        }

        Tip tip = tipRepository.findById(request.getTipId())
                .orElseThrow(() -> new IllegalArgumentException("팁을 찾을 수 없습니다."));

        // 소유자 확인
        if (!Objects.equals(tip.getUser().getNo(), loginUserNo)) {
            throw new AccessDeniedException("해당 팁에 대한 권한이 없습니다.");
        }

        Storage storage = storageRepository.findById(request.getStorageId())
                .orElseThrow(() -> new IllegalArgumentException("보관함을 찾을 수 없습니다."));

        // 보관함 소유 확인(그룹보관함이면 별도 권한 체크 로직이 있을 수 있음)
        if (storage.getUser() != null && !Objects.equals(storage.getUser().getNo(), loginUserNo)) {
            throw new AccessDeniedException("해당 보관함에 등록할 권한이 없습니다.");
        }

        // 매핑 저장(이미 있으면 무시)
        if (!storageTipRepository.existsByStorageAndTip(storage, tip)) {
            StorageTip st = new StorageTip();
            st.setStorage(storage);
            st.setTip(tip);
            storageTipRepository.save(st);
        }

        // 공개 여부 반영
        if (request.getIsPublic() != null) {
            tip.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        }
        tip.setUpdatedAt(LocalDateTime.now());
        Tip saved = tipRepository.save(tip);

        // 피드 브로드캐스트: tip:update
        Map<String, Object> feed = Map.of(
                "type", "tip:update",
                "tipId", saved.getNo(),
                "author", saved.getUser().getNickname() != null ? saved.getUser().getNickname() : saved.getUser().getLoginId(),
                "tags", Optional.ofNullable(saved.getTipTags()).orElse(List.of())
                        .stream().map(tt -> tt.getTag().getName()).toList(),
                "createdAt", saved.getCreatedAt(),
                "v", "v1"
        );
        messagingTemplate.convertAndSend("/topic/feed", feed);

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
        // TipUpdateRequest에는 url 필드가 없으므로 URL 갱신 제거
        if (req.getIsPublic() != null) tip.setIsPublic(req.getIsPublic());
        if (StringUtils.hasText(req.getContentSummary())) tip.setContentSummary(req.getContentSummary());
        tip.setUpdatedAt(LocalDateTime.now());

        Tip saved = tipRepository.save(tip);

        // (선택) 수정 알림
        Map<String, Object> feed = Map.of(
                "type", "tip:update",
                "tipId", saved.getNo(),
                "author", saved.getUser().getNickname() != null ? saved.getUser().getNickname() : saved.getUser().getLoginId(),
                "tags", Optional.ofNullable(saved.getTipTags()).orElse(List.of())
                        .stream().map(tt -> tt.getTag().getName()).toList(),
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

        // 레포에 deleteByTip... 메서드들(특히 StorageTipRepository/BookmarkRepository)이 없는 상태라면
        // 영속성 전이/외래키 제약에 맞춰 필요 시 별도 정리 메서드를 레포에 추가하세요.
        // (여기서는 컴파일 에러를 제거하고 엔티티 연관 설정에 위임)
        tipRepository.delete(tip);
    }

    /* =========================================================
       4) 자동 생성+등록 (비동기 큐 태우고 보관함까지 연결)
       ========================================================= */
    @Transactional
    public TipResponse createAndRegisterTipAutoAsync(Long loginUserNo, TipAutoCreateRequest request) {
        // TipAutoCreateRequest는 url, storageId 중심 → 나머지는 기본값 처리
        TipRequest createReq = new TipRequest();
        createReq.setUrl(request.getUrl());
        TipResponse created = generate(loginUserNo, createReq);

        TipRegisterRequest reg = new TipRegisterRequest();
        // 현재 표준 필드명은 tipId/storageId
        reg.setTipId(created.getNo());
        reg.setStorageId(request.getStorageId());
        // 공개 여부는 기본 true로 등록(필요 시 정책에 맞게 조정)
        reg.setIsPublic(true);

        return registerTip(reg, loginUserNo);
    }
}
