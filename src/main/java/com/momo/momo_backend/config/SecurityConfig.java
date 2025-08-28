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
                            // 1) 공개 엔드포인트
                            .requestMatchers(
                                    "/api/auth/**",
                                    "/swagger-ui/**",
                                    "/v3/api-docs/**",
                                    "/api/tips/tag/**",
                                    "/api/tips/public",
                                    "/api/query/tips/all",     // 전체 공개 목록
                                    "/api/query/tips/*",       // 상세 (id 한 세그먼트)
                                    "/api/search/tips/public",
                                    "/api/search/tips/tag/**",
                                    "/api/tips/*",             // TipController 상세
                                    "/ws/**"                   // STOMP 핸드셰이크
                            ).permitAll()

                            // 2) 보호가 필요한 조회(순서 중요: permitAll 전에 오면 더 안전)
                            .requestMatchers(
                                    "/api/query/tips/my",
                                    "/api/query/tips/storage/**"
                            ).authenticated()

                            // 3) 나머지는 인증
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