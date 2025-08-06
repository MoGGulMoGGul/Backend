package com.momo.momo_backend.security;

import com.momo.momo_backend.entity.User;
import com.momo.momo_backend.entity.UserCredential;
import com.momo.momo_backend.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username = 로그인 ID

        UserCredential credential = credentialRepository.findByLoginId(username)
                .orElseThrow(() -> new UsernameNotFoundException("로그인 정보를 찾을 수 없습니다: " + username));

        User user = userRepository.findById(credential.getUser().getNo())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 정보를 찾을 수 없습니다: " + credential.getUser().getNo()));

        return new CustomUserDetails(user, credential);
    }
}
