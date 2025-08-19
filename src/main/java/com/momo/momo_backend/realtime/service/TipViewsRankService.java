package com.momo.momo_backend.realtime.service;

import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.realtime.RealtimeProperties;
import com.momo.momo_backend.realtime.dto.EventTypes;
import com.momo.momo_backend.realtime.dto.TipViewsRankEvent;
import com.momo.momo_backend.repository.TipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        redis.opsForZSet().incrementScore(KEY, String.valueOf(tipId), 1.0);
    }

    /** 1.5초마다 상위 N 브로드캐스트 */
    @Scheduled(fixedDelay = 1500)
    public void broadcastTopN() {
        var tuples = redis.opsForZSet().reverseRangeWithScores(KEY, 0, TOP_N - 1);
        if (tuples == null || tuples.isEmpty()) return;

        List<TipViewsRankEvent.Item> items = new ArrayList<>();
        for (var t : tuples) {
            String tipIdStr = t.getValue();
            Double score = Objects.requireNonNullElse(t.getScore(), 0.0);
            Long tipId = Long.valueOf(tipIdStr);
            String title = tipRepository.findById(tipId)
                    .map(Tip::getTitle)
                    .orElse("제목 없음");
            items.add(TipViewsRankEvent.Item.builder()
                    .tipId(tipId).title(title).score(score).build());
        }

        var evt = TipViewsRankEvent.builder()
                .type(EventTypes.TIP_VIEWS_RANK_UPDATE) // "tip:views:rank:update"
                .leaderboard(items)
                .v(props.getSchemaVersion())
                .build();

        // properties에서 주입한 목적지로 전송
        messaging.convertAndSend(props.getTopics().getTipViewsRank(), evt);
    }
}
