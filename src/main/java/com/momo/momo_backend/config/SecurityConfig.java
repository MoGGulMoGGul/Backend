package com.momo.momo_backend.config;

import com.momo.momo_backend.security.CustomUserDetailsService;
import com.momo.momo_backend.security.JwtAuthenticationFilter;
import com.momo.momo_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** AuthenticationManager (폼로그인 미사용이더라도 PasswordEncoder 연결용) */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder());
        return builder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // JWT 환경: CSRF 비활성 + 세션 사용 안 함
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인가 규칙
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 공개 엔드포인트
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/ws/**",

                                // 공개 조회/검색
                                "/api/query/tips/all",
                                "/api/query/tips/*",
                                "/api/search/tips/public",
                                "/api/search/tips/tag/**",
                                "/api/bookmark/ranking/weekly",
                                "/api/users/all",
                                "/api/users/search",
                                "/api/tips/tag/**",
                                "/api/tips/public",
                                "/api/query/tips/{tipNo}",
                                "/api/bookmark/user/*/total-count",
                                "/api/query/tips/user/*",
                                "/api/tips/internal/tips/update-from-ai"
                        ).permitAll()

                        // 팁 상세 GET은 공개
                        .requestMatchers(HttpMethod.GET, "/api/tips/*").permitAll()

                        // 나머지는 인증
                        .anyRequest().authenticated()
                )

                // JWT 필터 연결 (UsernamePasswordAuthenticationFilter 앞)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
