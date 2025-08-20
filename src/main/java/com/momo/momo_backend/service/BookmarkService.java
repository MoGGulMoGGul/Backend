package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.BookmarkAndSaveRequest;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.enums.NotificationType;
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

    // 즐겨찾기 삭제 메서드
    @Transactional
    public void delete(Long bookmarkNo, Long userNo) {
        // 1. 즐겨찾기 정보를 찾고 소유권을 확인합니다.
        Bookmark bookmark = bookmarkRepository.findByNoAndUser_No(bookmarkNo, userNo)
                .orElseThrow(() -> new AccessDeniedException("해당 즐겨찾기를 삭제할 권한이 없거나, 즐겨찾기가 존재하지 않습니다."));

        // 2. 즐겨찾기에서 꿀팁과 사용자 정보를 가져옵니다.
        Tip tipToDelete = bookmark.getTip();
        User user = bookmark.getUser();

        // 3. 사용자의 모든 보관함에 있는 해당 꿀팁 정보를 삭제합니다.
        storageTipRepository.deleteByTipAndUser(tipToDelete, user);

        // 4. 마지막으로 즐겨찾기 자체를 삭제합니다.
        bookmarkRepository.delete(bookmark);
    }

    // 꿀팁을 즐겨찾기하고 선택한 보관함에 저장하는 메서드
    @Transactional
    public void bookmarkAndSaveTip(BookmarkAndSaveRequest request, Long userNo) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Tip tip = tipRepository.findById(request.getTipNo())
                .orElseThrow(() -> new IllegalArgumentException("꿀팁을 찾을 수 없습니다."));
        Storage storage = storageRepository.findById(request.getStorageNo())
                .orElseThrow(() -> new IllegalArgumentException("보관함을 찾을 수 없습니다."));

        // 보관함이 요청한 사용자의 소유인지 확인
        if (!storage.getUser().getNo().equals(userNo)) {
            throw new AccessDeniedException("해당 보관함에 저장할 권한이 없습니다.");
        }

        // 1. 즐겨찾기 추가 (이미 되어있지 않은 경우)
        if (!bookmarkRepository.existsByUserAndTip(user, tip)) {
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .tip(tip)
                    .createdAt(LocalDateTime.now())
                    .build();
            bookmarkRepository.save(bookmark);
            notifyTipOwnerOfBookmark(tip, user); // 꿀팁 원작자에게 알림 전송
        }

        // 2. 선택한 보관함에 꿀팁 저장 (이미 저장되어있지 않은 경우)
        if (!storageTipRepository.existsByStorageAndTip(storage, tip)) {
            StorageTip storageTip = StorageTip.builder()
                    .storage(storage)
                    .tip(tip)
                    .build();
            storageTipRepository.save(storageTip);
        }
    }


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
