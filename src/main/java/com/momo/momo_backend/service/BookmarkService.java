package com.momo.momo_backend.service;

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

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final TipRepository tipRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public void addBookmark(Long tipId, User user) {
        Tip tip = tipRepository.findById(tipId)
                .orElseThrow(() -> new IllegalArgumentException("해당 꿀팁을 찾을 수 없습니다."));

        if (bookmarkRepository.existsByUserAndTip(user, tip)) return;

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .tip(tip)
                .build();
        bookmarkRepository.save(bookmark);

        notifyTipOwnerOfBookmark(tip, user);
    }

    private void notifyTipOwnerOfBookmark(Tip tip, User bookmarker) {
        User tipOwner = tip.getUser();
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
