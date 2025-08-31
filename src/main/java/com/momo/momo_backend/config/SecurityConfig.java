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

// ⬇️ CORS 관련 import 추가
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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

    // ✅ CORS 허용 오리진/헤더/메서드 명시
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();

        // 프론트 오리진들만 넣는다 (API/백엔드 도메인은 넣지 않음)
        c.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://moggulmoggul-frontend.s3-website.ap-northeast-2.amazonaws.com",
                // 추후 CloudFront/커스텀 도메인 쓰면 여기에 추가: "https://app.moggulmoggul.com" 등
                "https://moggulmoggul-frontend.s3-website.ap-northeast-2.amazonaws.com"
        ));
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With"));
        // 토큰/쿠키를 쓸 가능성이 있으면 true
        c.setAllowCredentials(true);
        // 필요 시 노출 헤더
        c.setExposedHeaders(List.of("Location","Authorization"));

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
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
