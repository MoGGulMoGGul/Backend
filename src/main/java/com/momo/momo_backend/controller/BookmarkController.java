package com.momo.momo_backend.controller;

import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.dto.BookmarkRequest;
import com.momo.momo_backend.entity.Bookmark;
import com.momo.momo_backend.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    // 즐겨찾기 발생 시 알람용
    @PostMapping("/{tipId}")
    public ResponseEntity<Void> bookmarkTip(@PathVariable Long tipId,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        bookmarkService.addBookmark(tipId, userDetails.getUser());
        return ResponseEntity.ok().build();
    } // 메서드 닫는 중괄호 추가

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