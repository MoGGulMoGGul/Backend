package com.momo.momo_backend.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 팁 관련 주기 작업(임시 팁 청소 등)을 묶어두는 스케줄러.
 * 현재는 안전한 하트비트 로그만 남깁니다.
 * 실제 정리 로직은 TipService 쪽 정리 메서드가 준비되면 여기서 호출하세요.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TipCleanupScheduler {

    // private final TipService tipService;  // 정리 메서드가 준비되면 주입 후 사용

    /** 5분마다 하트비트 (운영시 cron 주기로 변경 가능) */
    @Scheduled(cron = "0 */5 * * * *")
    public void heartbeat() {
        log.debug("[TipCleanupScheduler] heartbeat");
        // 예시) 준비되면 사용:
        // tipService.cleanUpTemporaryTips();
        // tipService.syncExternalSummaries();
    }
}
