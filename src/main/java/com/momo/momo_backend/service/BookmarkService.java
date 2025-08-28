package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.BookmarkAndSaveRequest;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        notifyTipOwnerOfBookmark(tip, user); // 알림 전송
    }

    /** 즐겨찾기 삭제 */
    @Transactional
    public void delete(Long bookmarkNo, Long userNo) {
        Bookmark bookmark = bookmarkRepository.findByNoAndUser_No(bookmarkNo, userNo)
                .orElseThrow(() -> new AccessDeniedException("해당 즐겨찾기를 삭제할 권한이 없거나, 즐겨찾기가 존재하지 않습니다."));

        Tip tipToDelete = bookmark.getTip();
        User user = bookmark.getUser();

        // 사용자의 모든 보관함에서 해당 팁을 제거 (레포에 메서드가 존재한다고 가정)
        storageTipRepository.deleteByTipAndUser(tipToDelete, user);

        bookmarkRepository.delete(bookmark);
    }

    /** 북마크 + 특정 보관함 저장 */
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

        // 1) 북마크 없으면 추가
        if (!bookmarkRepository.existsByUserAndTip(user, tip)) {
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .tip(tip)
                    .createdAt(LocalDateTime.now())
                    .build();
            bookmarkRepository.save(bookmark);
            notifyTipOwnerOfBookmark(tip, user);
        }

        // 2) 보관함에 저장(중복 체크)
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

        // 자기 자신의 팁을 북마크한 경우 알림 생략
        if (tipOwner.getNo().equals(bookmarker.getNo())) return;

        String msg = (bookmarker.getNickname() != null ? bookmarker.getNickname() : bookmarker.getLoginId())
                + " 님이 내 꿀팁을 북마크했습니다.";

        // 현 Notification 엔티티 시그니처: (user, type(String), message, linkUrl)
        Notification notification = Notification.of(
                tipOwner,
                "BOOKMARKED_MY_TIP",
                msg,
                null // 필요하면 상세 링크 전달
        );

        notificationRepository.save(notification);
    }
}
