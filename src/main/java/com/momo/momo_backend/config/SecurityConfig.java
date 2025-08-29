package com.momo.momo_backend.config;

import com.momo.momo_backend.security.CustomUserDetailsService;
import com.momo.momo_backend.security.JwtAuthenticationFilter;
import com.momo.momo_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF(Cross-Site Request Forgery) 보호 기능을 비활성화합니다.
                .csrf(csrf -> csrf.disable())

                // 세션 관리를 STATELESS로 설정하여 JWT 인증에 적합하게 만듭니다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CORS 설정을 활성화합니다. 아래의 corsConfigurationSource Bean을 사용합니다.
                .cors(Customizer.withDefaults())

                // HTTP 요청에 대한 접근 권한 설정
                .authorizeHttpRequests(authz -> authz
                        // Preflight 요청은 인증 없이 허용합니다.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 아래에 명시된 API 경로는 인증 없이 누구나 접근 가능하도록 허용합니다.
                        .requestMatchers(
                                "/api/auth/**",             // 로그인/회원가입
                                "/swagger-ui/**",           // Swagger UI
                                "/v3/api-docs/**",          // Swagger API 문서
                                "/api/tips/tag/**",
                                "/api/tips/public",         // 공개 팁 조회
                                "/api/query/tips/all",      // 전체 공개 꿀팁 조회
                                "/api/query/tips/{tipNo}",  // 상세 팁 조회
                                "/api/search/tips/public",  // 공개 팁 검색
                                "/api/search/tips/tag/**",  // 태그로 공개 팁 검색
                                "/api/bookmark/ranking/weekly",             // 주간 북마크 랭킹 조회
                                "/api/bookmark/user/{userNo}/total-count",  // 특정 사용자의 총 북마크 수 조회
                                "/api/users/search",                        // 사용자 아이디 검색
                                "/api/query/tips/user/{userNo}",            // 특정 사용자 공개 꿀팁 조회
                                "/api/tips/internal/tips/update-from-ai"    // AI 콜백을 위한 경로 추가
                        ).permitAll()
                        // 그 외의 모든 요청은 인증된 사용자만 접근 가능
                        .anyRequest().authenticated()
                )

                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가합니다.
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService, redisTemplate),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
