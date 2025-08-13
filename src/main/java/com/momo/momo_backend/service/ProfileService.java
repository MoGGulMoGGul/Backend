package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.ProfileUpdateResponse;
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
    public ProfileUpdateResponse updateProfile(Long userNo, String nickname, MultipartFile imageFile) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1. 닉네임 수정 로직 (닉네임이 요청에 포함된 경우)
        if (nickname != null && !nickname.isBlank()) {
            // 변경할 닉네임이 현재 닉네임과 다를 경우에만 중복 검사 수행
            if (!user.getNickname().equals(nickname) && userRepository.findByNickname(nickname).isPresent()) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
            user.setNickname(nickname);
        }

        String newProfileImageUrl = user.getProfileImage();

        // 2. 프로필 이미지 수정 로직 (이미지 파일이 요청에 포함된 경우)
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String originalFileName = imageFile.getOriginalFilename();
                String extension = "";
                if (originalFileName != null && originalFileName.contains(".")) {
                    extension = originalFileName.substring(originalFileName.lastIndexOf("."));
                }
                String savedFileName = UUID.randomUUID().toString() + extension;
                Path savedPath = this.uploadPath.resolve(savedFileName);

                imageFile.transferTo(savedPath.toFile());

                newProfileImageUrl = "/uploads/" + savedFileName; // 웹 접근 경로
                user.setProfileImage(newProfileImageUrl);
            } catch (IOException e) {
                throw new RuntimeException("프로필 이미지 저장에 실패했습니다.", e);
            }
        }

        userRepository.save(user);

        // 클라이언트에 반환할 응답 생성
        return ProfileUpdateResponse.builder()
                .message("프로필이 성공적으로 수정되었습니다.")
                .profileImageUrl(newProfileImageUrl)
                .build();
    }
}