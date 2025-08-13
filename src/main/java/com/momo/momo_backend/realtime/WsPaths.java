package com.momo.momo_backend.realtime;

public final class WsPaths {
    private WsPaths() {}

    public static final String STOMP_ENDPOINT = "/ws";
    public static final String APP_PREFIX = "/app";
    public static final String USER_DEST_PREFIX = "/user";

    public static final String TOPIC_FEED = "/topic/feed";
    public static final String TOPIC_CHALLENGE_RANK_FMT = "/topic/challenge/%d/rank"; // String.format 사용

    public static final String USER_QUEUE_NOTIFICATIONS = "/queue/notifications";
}
