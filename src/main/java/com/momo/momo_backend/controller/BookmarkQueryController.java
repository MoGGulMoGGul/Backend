package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.WeeklyRankingResponse;
import com.momo.momo_backend.service.BookmarkQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookmark")
@RequiredArgsConstructor
public class BookmarkQueryController {

    private final BookmarkQueryService bookmarkQueryService;

    // 주간 북마크 랭킹 조회
    @GetMapping("/ranking/weekly")
    public ResponseEntity<List<WeeklyRankingResponse>> getWeeklyRanking() {
        List<WeeklyRankingResponse> ranking = bookmarkQueryService.getWeeklyBookmarkRanking();
        return ResponseEntity.ok(ranking);
    }
}