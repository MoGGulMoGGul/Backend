package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.BookmarkRequest;
import com.momo.momo_backend.entity.Bookmark;
import com.momo.momo_backend.entity.Notification;
import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.enums.NotificationType;
import com.momo.momo_backend.repository.BookmarkRepository;
import com.momo.momo_backend.repository.NotificationRepository;
import com.momo.momo_backend.repository.TipRepository;
import com.momo.momo_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final TipRepository tipRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    /**
     * ✅ 즐겨찾기 등록 (DTO 기반 - BookmarkRequest 사용)
     * [출처: 첫 번째 코드]
     */
    public Bookmark save(BookmarkRequest request) {
        if (bookmarkRepository.existsByUser_NoAndTip_No(request.getUserNo(), request.getTipNo())) {
            throw new IllegalArgumentException("이미 즐겨찾기한 꿀팁입니다.");
        }

        User user = userRepository.findById(request.getUserNo())
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));
        Tip tip = tipRepository.findById(request.getTipNo())
                .orElseThrow(() -> new IllegalArgumentException("팁이 존재하지 않습니다."));

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .tip(tip)
                .createdAt(LocalDateTime.now())
                .build();

        bookmarkRepository.save(bookmark);
        notifyTipOwnerOfBookmark(tip, user); // 알림 전송

        return bookmark;
    }

    /**
     * ✅ 즐겨찾기 등록 (엔티티 기반 - 로그인 사용자 전달)
     * [출처: 두 번째 코드]
     */
    public void addBookmark(Long tipId, User user) {
        Tip tip = tipRepository.findById(tipId)
                .orElseThrow(() -> new IllegalArgumentException("해당 꿀팁을 찾을 수 없습니다."));

        if (bookmarkRepository.existsByUserAndTip(user, tip)) return;

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .tip(tip)
                .createdAt(LocalDateTime.now())
                .build();

        bookmarkRepository.save(bookmark);
        notifyTipOwnerOfBookmark(tip, user); // 알림 전송
    }

    /**
     * ✅ 즐겨찾기 삭제
     * [출처: 첫 번째 코드]
     */
    public void delete(Long bookmarkNo) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkNo)
                .orElseThrow(() -> new IllegalArgumentException("즐겨찾기가 존재하지 않습니다."));

        bookmarkRepository.delete(bookmark);
    }

    /**
     * ✅ 즐겨찾기 알림 전송
     * [출처: 두 번째 코드]
     */
    private void notifyTipOwnerOfBookmark(Tip tip, User bookmarker) {
        User tipOwner = tip.getUser();

        // 본인이 자기 꿀팁을 북마크한 경우 알림 생략
        if (tipOwner.getNo().equals(bookmarker.getNo())) return;

        Notification notification = Notification.builder()
                .receiver(tipOwner)
                .tip(tip)
                .type(NotificationType.BOOKMARKED_MY_TIP)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }
}
