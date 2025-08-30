package com.momo.momo_backend.realtime.support;

public interface TipQueryPort {
    TipSummaryView findSummaryById(Long tipId);
}
