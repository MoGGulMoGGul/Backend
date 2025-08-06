package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.BookmarkRequest;
import com.momo.momo_backend.entity.Bookmark;
import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.repository.BookmarkRepository;
import com.momo.momo_backend.repository.TipRepository;
import com.momo.momo_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final TipRepository tipRepository;

    // 즐겨찾기 저장
    public Bookmark save(BookmarkRequest request) {
        if (bookmarkRepository.existsByUser_NoAndTip_No(request.getUserNo(), request.getTipNo())) {
            throw new IllegalArgumentException("이미 즐겨찾기한 꿀팁입니다.");
        }

        User user = userRepository.findById(request.getUserNo())
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));
        Tip tip = tipRepository.findById(request.getTipNo())
                .orElseThrow(() -> new IllegalArgumentException("팁이 존재하지 않습니다."));

        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setTip(tip);
        bookmark.setCreatedAt(LocalDateTime.now());

        return bookmarkRepository.save(bookmark);
    }

    // 즐겨찾기 삭제
    public void delete(Long bookmarkNo) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkNo)
                .orElseThrow(() -> new IllegalArgumentException("즐겨찾기가 존재하지 않습니다."));

        bookmarkRepository.delete(bookmark);
    }
}