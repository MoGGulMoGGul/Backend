package com.momo.momo_backend.controller;

import com.momo.momo_backend.security.CustomUserDetails;
import com.momo.momo_backend.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/{tipId}")
    public ResponseEntity<Void> bookmarkTip(@PathVariable Long tipId,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        bookmarkService.addBookmark(tipId, userDetails.getUser());
        return ResponseEntity.ok().build();
    }
}
