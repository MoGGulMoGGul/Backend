package com.momo.momo_backend.scheduler;

import com.momo.momo_backend.service.TipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TipCleanupScheduler {

    private final TipService tipService;

    // 매일 새벽 1시 0분에 실행 (초 분 시 일 월 요일)
    // 예: "0 0 1 * * ?" -> 매일 01:00:00
    // 더 자주 테스트하려면: "0 */1 * * * ?" -> 매 1분마다
    // 또는 "0 0/5 * * * ?" -> 매 5분마다
    @Scheduled(cron = "0 0 1 * * ?") // 매일 새벽 1시에 실행
    public void cleanupOldUnregisteredTips() {
        log.info("스케줄링된 꿀팁 정리 작업 실행.");
        tipService.deleteOldUnregisteredTips();
    }
}
// 이 스케줄러는 매일 새벽 1시에 실행되어, 등록되지 않은 꿀팁 중 생성된 지 24시간이 지난 팁을 삭제합니다.