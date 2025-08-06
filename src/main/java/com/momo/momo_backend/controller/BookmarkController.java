package com.momo.momo_backend.controller;

import com.momo.momo_backend.dto.BookmarkRequest;
import com.momo.momo_backend.entity.Bookmark;
import com.momo.momo_backend.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookmark")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    // 북마크 생성
    @PostMapping
    public ResponseEntity<Bookmark> save(@RequestBody BookmarkRequest request) {
        Bookmark saved = bookmarkService.save(request);
        return ResponseEntity.ok(saved);
    }

    // 북마크 삭제
    @DeleteMapping("/{bookmarkNo}")
    public ResponseEntity<Void> delete(@PathVariable Long bookmarkNo) {
        bookmarkService.delete(bookmarkNo);
        return ResponseEntity.noContent().build(); // 204 응답
    }
}