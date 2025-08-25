package com.momo.momo_backend.realtime.security;

import com.momo.momo_backend.security.JwtTokenProvider;
import com.momo.momo_backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null) return message;

        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            // 1) 토큰 추출
            String token = resolveBearer(getFirstNativeHeader(acc, "Authorization"));
            if (!StringUtils.hasText(token)) {
                token = resolveBearer(getFirstNativeHeader(acc, "authorization"));
            }
            if (!StringUtils.hasText(token)) {
                throw new IllegalArgumentException("Authorization header missing on STOMP CONNECT");
            }

            if (!jwtTokenProvider.validateToken(token)) {
                throw new IllegalArgumentException("Invalid JWT on STOMP CONNECT");
            }

            // 2) Principal 이름을 "userNo" 문자열로 고정
            String nameForUserDest = null;

            // 우선 Authentication을 뽑을 수 있으면 활용
            try {
                var auth = jwtTokenProvider.getAuthentication(token);
                if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
                    // CustomUserDetails 에 getNo()가 없으므로, 엔티티 통해 조회
                    if (cud.getUser() != null) {
                        nameForUserDest = String.valueOf(cud.getUser().getNo());
                    }
                }
            } catch (Throwable ignore) { /* fallback 아래로 */ }

            // fallback: 토큰에서 userNo 직접 추출
            if (!StringUtils.hasText(nameForUserDest)) {
                Long userNo = jwtTokenProvider.getUserNo(token); // 아래 B-2 메서드 추가
                nameForUserDest = String.valueOf(userNo);
            }

            Principal principal = new UsernamePasswordAuthenticationToken(
                    nameForUserDest, null, null
            );
            acc.setUser(principal);
        }
        return message;
    }

    private static String getFirstNativeHeader(StompHeaderAccessor acc, String key) {
        List<String> vals = acc.getNativeHeader(key);
        return (vals != null && !vals.isEmpty()) ? vals.get(0) : null;
    }
    private static String resolveBearer(String header) {
        if (!StringUtils.hasText(header)) return null;
        return header.startsWith("Bearer ") ? header.substring(7) : header;
    }
}
