package com.momo.momo_backend.realtime.support;

import java.time.Instant;
import java.util.List;

public record TipSummaryView(
        Long id,
        String title,
        String author,
        List<String> tags,
        Instant createdAt,
        String thumbnailUrl
) {}
