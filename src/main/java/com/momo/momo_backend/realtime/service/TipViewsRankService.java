package com.momo.momo_backend.realtime.service;

import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.realtime.RealtimeProperties;
import com.momo.momo_backend.realtime.dto.EventTypes;
import com.momo.momo_backend.realtime.dto.TipViewsRankEvent;
import com.momo.momo_backend.repository.TipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipViewsRankService {

    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate messaging;
    private final RealtimeProperties props;
    private final TipRepository tipRepository;

    private static final String KEY = "tips:views:score"; // 단일 글로벌 랭킹
    private static final int TOP_N = 10;

    /** 조회 1회 기록 */
    public void recordView(long tipId) {
        try {
            redis.opsForZSet().incrementScore(KEY, String.valueOf(tipId), 1.0);
        } catch (Exception e) {
            log.warn("뷰 카운트 적재 실패 tipId={}, err={}", tipId, e.getMessage());
        }
    }

    /** 1.5초마다 상위 N 브로드캐스트 */
    @Scheduled(fixedDelay = 1500)
    public void broadcastTopN() {
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redis.opsForZSet().reverseRangeWithScores(KEY, 0, TOP_N - 1);

            if (tuples == null || tuples.isEmpty()) return;

            // 1) tipId 목록 추출
            List<Long> ids = tuples.stream()
                    .map(t -> Long.valueOf(Objects.requireNonNull(t.getValue())))
                    .toList();

            // 2) 제목을 배치로 미리 조회(쿼리 1회)
            Map<Long, String> titleMap = tipRepository.findAllById(ids).stream()
                    .collect(Collectors.toMap(
                            Tip::getNo,
                            t -> Optional.ofNullable(t.getTitle()).orElse("제목 없음")
                    ));

            // 3) 순서 보존하며 payload 구성
            List<TipViewsRankEvent.Item> items = new ArrayList<>();
            for (ZSetOperations.TypedTuple<String> t : tuples) {
                Long tipId = Long.valueOf(Objects.requireNonNull(t.getValue()));
                Double score = Objects.requireNonNullElse(t.getScore(), 0.0);
                String title = titleMap.getOrDefault(tipId, "제목 없음");

                items.add(TipViewsRankEvent.Item.builder()
                        .tipId(tipId)
                        .title(title)
                        .score(score)
                        .build());
            }

            TipViewsRankEvent evt = TipViewsRankEvent.builder()
                    .type(EventTypes.TIP_VIEWS_RANK_UPDATE) // "tip:views:rank:update"
                    .leaderboard(items)
                    .v(props.getSchemaVersion())
                    .build();

            // properties에서 주입한 목적지로 전송
            messaging.convertAndSend(props.getTopics().getTipViewsRank(), evt);

        } catch (Exception e) {
            log.warn("랭킹 브로드캐스트 실패: {}", e.getMessage());
        }
    }
}
