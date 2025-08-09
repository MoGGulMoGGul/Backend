package com.momo.momo_backend.security;

import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.entity.UserCredential;
import com.momo.momo_backend.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserCredentialRepository credentialRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username = loginId (예: testuser)
        UserCredential credential = credentialRepository.findByLoginId(username)
                .orElseThrow(() -> new UsernameNotFoundException("로그인 정보를 찾을 수 없습니다: " + username));

        User user = credential.getUser();  //추가 쿼리 없이 연관된 User 사용

        return new CustomUserDetails(user, credential);
    }
}
