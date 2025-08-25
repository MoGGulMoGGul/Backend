package com.momo.momo_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-validity-ms:3600000}") long accessTokenValidityMs,      // 기본 1시간
            @Value("${jwt.refresh-validity-ms:1209600000}") long refreshTokenValidityMs // 기본 14일
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    /* ===================== 토큰 생성 ===================== */

    /** access token: subject=loginId, claim=userNo */
    public String createAccessToken(Long userNo, String loginId) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiry = new Date(now + accessTokenValidityMs);

        return Jwts.builder()
                .setClaims(Map.of("userNo", userNo))
                .setSubject(loginId)
                .setIssuedAt(issuedAt)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** refresh token: subject=loginId, claim=userNo (재발급시 파싱용) */
    public String createRefreshToken(Long userNo, String loginId) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiry = new Date(now + refreshTokenValidityMs);

        return Jwts.builder()
                .setClaims(Map.of("userNo", userNo))
                .setSubject(loginId)
                .setIssuedAt(issuedAt)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /* ===================== 파싱/검증 ===================== */

    public boolean validateToken(String tokenOrBearer) {
        try {
            String token = resolveBearer(tokenOrBearer);
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT invalid: {}", e.getMessage());
            return false;
        }
    }

    public Long getUserNo(String tokenOrBearer) {
        Claims claims = parseClaims(resolveBearer(tokenOrBearer));
        Object val = claims.get("userNo");
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return null; }
    }

    /** 기존 코드 호환용: subject=loginId 를 반환 */
    public String getUserIdFromToken(String tokenOrBearer) {
        return getLoginId(tokenOrBearer);
    }

    public String getLoginId(String tokenOrBearer) {
        Claims claims = parseClaims(resolveBearer(tokenOrBearer));
        return claims.getSubject();
    }

    public Claims parseClaims(String rawToken) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(rawToken)
                .getBody();
    }

    /* ===================== 요청/헤더 유틸 ===================== */

    /** 기존 필터 호환: Authorization 헤더에서 Bearer 토큰 추출 */
    @Nullable
    public String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = resolveBearer(header);
        return (token != null && !token.isBlank()) ? token : null;
    }

    /** "Bearer xxx" 문자열/토큰 혼용 입력을 안전하게 정리 */
    @Nullable
    public String resolveBearer(@Nullable String headerOrToken) {
        if (headerOrToken == null) return null;
        String v = headerOrToken.trim();
        if (v.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return v.substring(7).trim();
        }
        return v;
    }

    /* ===================== 선택: Authentication 제공 ===================== */
    /**
     * STOMP 인터셉터가 호출하지만, 현재는 claim 기반 fallback 을 쓰므로 null 반환해도 무방.
     * (원하면 CustomUserDetailsService 로 사용자 로딩해서 채워도 됨)
     */
    @Nullable
    public Authentication getAuthentication(String tokenOrBearer) {
        return null;
    }
}
