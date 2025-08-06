package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.TipRequest;
import com.momo.momo_backend.dto.TipResponse;
import com.momo.momo_backend.dto.TipUpdateRequest;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TipService {
    private final TipRepository tipRepository;
    private final TagRepository tagRepository;
    private final TipTagRepository tipTagRepository;
    private final UserRepository userRepository;
    private final StorageRepository storageRepository;

    // 팁 생성
    public Tip createTip(Long userNo, TipRequest request) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Storage storage = storageRepository.findById(request.getStorageId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 보관함입니다."));

        Tip tip = new Tip();
        tip.setUser(user);
        tip.setUrl(request.getUrl());
        tip.setTitle(request.getTitle()); // 사용자가 입력하지 않으면 null로 저장
        tip.setIsPublic(request.getIsPublic());
        tip.setCreatedAt(LocalDateTime.now());
        tip.setUpdatedAt(LocalDateTime.now());

        // 저장 (tags는 register 단계에서 처리)
        return tipRepository.save(tip);
    }

    // 팁 등록 (FastAPI 서버에 URL 전달하여 요약 생성)
    public Tip registerTip(Long tipId) {
        Tip tip = tipRepository.findById(tipId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 tip입니다."));

        // FastAPI 서버 URL 호출
        RestTemplate restTemplate = new RestTemplate();
        String pythonUrl = "http://localhost:8000/async-index/";  // 실제 환경에 따라 수정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> request = Map.of("url", tip.getUrl());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(pythonUrl, entity, Map.class);
            String taskId = (String) response.getBody().get("task_id");

            // (선택) taskId를 Tip 객체에 저장하고 추후 비동기 상태 추적 가능
            tip.setContentSummary("요약 생성 중... (taskId: " + taskId + ")");
            tip.setUpdatedAt(LocalDateTime.now());
            return tipRepository.save(tip);

        } catch (Exception e) {
            throw new RuntimeException("FastAPI 요약 처리 요청 실패", e);
        }
    }

    // 팁 수정
    @Transactional
    public TipResponse update(Long tipNo, Long userNo, TipUpdateRequest req) {
        // 1) 소유자 검증 포함 조회
        Tip tip = tipRepository.findByNoAndUser_No(tipNo, userNo)
                .orElseThrow(() -> new AccessDeniedException("수정 권한이 없습니다."));

        // 2) 본문 필드 반영 (null 허용 필드는 null일 때 미수정)
        if (req.getTitle() != null) tip.setTitle(req.getTitle());
        if (req.getContentSummary() != null) tip.setContentSummary(req.getContentSummary());
        if (req.getIsPublic() != null) tip.setIsPublic(req.getIsPublic());

        // 3) 태그 동기화 (이름 기준, 전체 교체)
        if (req.getTags() != null) {
            // 기존 연결 제거
            tipTagRepository.deleteByTipNo(tipNo);
            tip.getTipTags().clear(); // 양방향 컬렉션 유지 시

            // 중복 제거 + 입력 순서 보존
            Set<String> names = new LinkedHashSet<>(req.getTags());

            for (String name : names) {
                Tag tag = tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
                TipTag tt = TipTag.builder().tip(tip).tag(tag).build();
                tip.getTipTags().add(tt); // 양방향 컬렉션 유지
            }
        }

        // 4) 응답 변환
        return TipResponse.from(tip);
    }

    // 팁 삭제
    @Transactional
    public void delete(Long tipNo, Long userNo) {
        Tip tip = tipRepository.findByNoAndUser_No(tipNo, userNo)
                .orElseThrow(() -> new AccessDeniedException("삭제 권한이 없습니다."));

        // FK 제약 고려: TipTag 먼저 제거(필요 시)
        tipTagRepository.deleteByTipNo(tipNo);
        tipRepository.delete(tip);
    }
}
