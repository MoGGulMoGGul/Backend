//package com.momo.momo_backend.config;
//
//import com.momo.momo_backend.security.JwtAuthenticationFilter;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//@Configuration
//@RequiredArgsConstructor
//@EnableMethodSecurity  // @PreAuthorize 등 활성화
//public class SecurityConfig {
//
//    private final JwtAuthenticationFilter jwtAuthenticationFilter;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        return http
//                .csrf(csrf -> csrf.disable())
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers(
//                                "/api/auth/**",         // 로그인/회원가입 허용
//                                "/swagger-ui/**",
//                                "/v3/api-docs/**",
//                                "/api/tips/tag/**",
//                                "/api/tips/public"
//                        ).permitAll()
//                        .requestMatchers("/api/query/**").permitAll()  // 팁 조회 API 허용
//
//                        .anyRequest().authenticated()  // 나머지 요청은 인증 필요
//                )
//                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//                .build();
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();  // 비밀번호 암호화
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
//        return configuration.getAuthenticationManager();  // 로그인 인증에 필요
//    }
//}

package com.momo.momo_backend.config;

import com.momo.momo_backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@RequiredArgsConstructor
// 일시적으로 Method Security 비활성화해서 테스트
// @EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("SecurityFilterChain 설정 중...");

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    log.info("HTTP 요청 권한 설정 중...");
                    auth
                            .requestMatchers(
                                    "/api/auth/**",         // 로그인/회원가입 허용
                                    "/swagger-ui/**",
                                    "/v3/api-docs/**",
                                    "/api/tips/tag/**",
                                    "/api/tips/public",     // 공개 팁 조회 허용
                                    "/api/query/tips/all", // 전체 공개 꿀팁 조회 허용
                                    "/api/query/tips/{tipId}",
                                    "/api/search/tips/public",
                                    "/api/search/tips/tag/**",// 상세 팁 조회 허용
                                    "/api/query/tips/{tipId}", // 상세 팁 조회 허용
                                    "/api/bookmark/ranking/weekly", // 주간 북마크 랭킹 조회 허용
                                    "/api/bookmark/user/{userNo}/total-count", // 특정 사용자의 총 북마크 수 조회 허용
                                    "/api/profile/{userNo}", // 프로필 조회 경로 허용
                                    "/api/users/all" // 모든 사용자 목록 조회 허용
                            ).permitAll()
                            .anyRequest().authenticated();  // 나머지 요청은 인증 필요
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // 비밀번호 암호화
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();  // 로그인 인증에 필요
    }
}