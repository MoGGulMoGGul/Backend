package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.ProfileImageUpdateResponse;
import com.momo.momo_backend.dto.ProfileUpdateRequest;
import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    // 이미지 업로드 경로 설정 (프로젝트 루트에 'uploads' 폴더 생성 필요)
    // 절대 경로를 사용하도록 수정
    private Path uploadPath;

    // 애플리케이션 시작 시 업로드 폴더 경로 설정
    @PostConstruct
    public void init() {
        // 사용자의 홈 디렉토리를 기준으로 'uploads' 폴더 경로 설정
        this.uploadPath = Paths.get(System.getProperty("user.home"), "uploads");
        try {
            // 애플리케이션 시작 시 'uploads' 폴더가 없으면 생성
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("업로드 폴더를 생성할 수 없습니다.", e);
        }
    }

    // 프로필 수정 메서드
    @Transactional
    public User updateUserProfile(Long userNo, ProfileUpdateRequest request) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String newNickname = request.getNickname();

        // 변경할 닉네임이 현재 닉네임과 다를 경우에만 중복 검사 수행
        if (!user.getNickname().equals(newNickname)) {
            Optional<User> existingUser = userRepository.findByNickname(newNickname);
            if (existingUser.isPresent()) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
        }

        user.setNickname(newNickname);
        return userRepository.save(user);
    }

    // 프로필 이미지 업데이트 메서드
    @Transactional
    public ProfileImageUpdateResponse updateUserProfileImage(Long userNo, MultipartFile imageFile) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어있습니다.");
        }

        try {
            String originalFileName = imageFile.getOriginalFilename();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String savedFileName = UUID.randomUUID().toString() + extension;

            // 절대 경로를 사용하여 파일 저장 위치 결정
            Path savedPath = this.uploadPath.resolve(savedFileName);

            // 파일 저장
            imageFile.transferTo(savedPath.toFile());

            // DB에 저장할 경로는 웹에서 접근 가능한 상대 경로로 유지
            String imageUrl = "/uploads/" + savedFileName;
            user.setProfileImage(imageUrl);
            userRepository.save(user);

            return ProfileImageUpdateResponse.builder()
                    .profileImageUrl(imageUrl)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("프로필 이미지 저장에 실패했습니다.", e);
        }
    }
}