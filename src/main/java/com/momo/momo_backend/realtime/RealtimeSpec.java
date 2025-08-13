package com.momo.momo_backend.realtime;

public final class RealtimeSpec {
    private RealtimeSpec() {}

    // 스키마 버전(프론트와 계약)
    public static final String SCHEMA_VERSION = "v1";

    // 이벤트 타입 (이름만 선점: 실제 DTO/발행은 이후 단계)
    public static final String EVT_TIP_NEW = "tip:new";
    public static final String EVT_CHALLENGE_RANK_UPDATE = "challenge:rank:update";
    public static final String EVT_NOTIFICATION_NEW = "notification:new";
}
