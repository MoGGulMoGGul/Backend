package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.BookmarkAndSaveRequest;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.enums.NotificationType;
import com.momo.momo_backend.repository.*;
import com.momo.momo_backend.realtime.events.NotificationCreatedEvent; // ✅ 추가
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;              // ✅ 추가
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;                                             // ✅ 추가
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final TipRepository tipRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final StorageRepository storageRepository;
    private final StorageTipRepository storageTipRepository;

    private final ApplicationEventPublisher eventPublisher;          // ✅ 추가

    /** 단순 북마크 추가 */
    public void addBookmark(Long tipNo, User user) {
        Tip tip = tipRepository.findById(tipNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 꿀팁을 찾을 수 없습니다."));

        if (bookmarkRepository.existsByUserAndTip(user, tip)) return;

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .tip(tip)
                .createdAt(LocalDateTime.now())
                .build();

        bookmarkRepository.save(bookmark);
        notifyTipOwnerOfBookmark(tip, user);
    }

    @Transactional
    public void delete(Long bookmarkNo, Long userNo) {
        Bookmark bookmark = bookmarkRepository.findByNoAndUser_No(bookmarkNo, userNo)
                .orElseThrow(() -> new AccessDeniedException("해당 즐겨찾기를 삭제할 권한이 없거나, 즐겨찾기가 존재하지 않습니다."));

        Tip tipToDelete = bookmark.getTip();
        User user = bookmark.getUser();

        // 구현되어 있다고 가정한 커스텀 삭제 로직
        storageTipRepository.deleteByTipAndUser(tipToDelete, user);

        bookmarkRepository.delete(bookmark);
    }

    @Transactional
    public void bookmarkAndSaveTip(BookmarkAndSaveRequest request, Long userNo) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Tip tip = tipRepository.findById(request.getTipNo())
                .orElseThrow(() -> new IllegalArgumentException("꿀팁을 찾을 수 없습니다."));
        Storage storage = storageRepository.findById(request.getStorageNo())
                .orElseThrow(() -> new IllegalArgumentException("보관함을 찾을 수 없습니다."));

        if (!storage.getUser().getNo().equals(userNo)) {
            throw new AccessDeniedException("해당 보관함에 저장할 권한이 없습니다.");
        }

        if (!bookmarkRepository.existsByUserAndTip(user, tip)) {
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .tip(tip)
                    .createdAt(LocalDateTime.now())
                    .build();
            bookmarkRepository.save(bookmark);
            notifyTipOwnerOfBookmark(tip, user);
        }

        if (!storageTipRepository.existsByStorageAndTip(storage, tip)) {
            StorageTip storageTip = StorageTip.builder()
                    .storage(storage)
                    .tip(tip)
                    .build();
            storageTipRepository.save(storageTip);
        }
    }

    /** 꿀팁 주인에게 '북마크됨' 알림 */
    private void notifyTipOwnerOfBookmark(Tip tip, User bookmarker) {
        User tipOwner = tip.getUser();
        if (tipOwner.getNo().equals(bookmarker.getNo())) return; // 자기 자신이면 생략

        Notification notification = Notification.of(
                tipOwner,                      // 수신자
                NotificationType.BOOKMARKED_MY_TIP,
                tip                            // 관련 팁(있음)
        );
        notificationRepository.save(notification);

        // ✅ 저장 직후, 개인 큐 전송용 이벤트 발행 (AFTER_COMMIT에서 리스너가 처리)
        String actorName = bookmarker.getNickname() != null ? bookmarker.getNickname() : bookmarker.getLoginId();
        String tipTitle  = tip.getTitle() != null ? tip.getTitle() : "제목 없음";
        String message   = actorName + "님이 당신의 꿀팁을 북마크했습니다: " + tipTitle;

        eventPublisher.publishEvent(
                new NotificationCreatedEvent(
                        tipOwner.getNo(),         // targetUserId (Long)
                        tip.getNo(),              // tipId (nullable 아님)
                        message,                  // message
                        Instant.now()             // createdAt
                )
        );
    }
}
