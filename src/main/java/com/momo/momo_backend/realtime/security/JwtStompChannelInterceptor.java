package com.momo.momo_backend.realtime.security;

import com.momo.momo_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand cmd = accessor.getCommand();

        if (cmd == StompCommand.CONNECT || cmd == StompCommand.SUBSCRIBE || cmd == StompCommand.SEND) {
            String token = extractBearer(accessor);
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // JwtTokenProvider에 getAuthentication()이 없으므로
                // 토큰의 subject(loginId)로 최소 권한 Principal 구성
                String loginId = jwtTokenProvider.getUserIdFromToken(token);
                if (loginId != null) {
                    Authentication auth =
                            new UsernamePasswordAuthenticationToken(loginId, null, List.of());
                    accessor.setUser(auth);
                    log.debug("STOMP {} -> authenticated user={}", cmd, loginId);
                } else {
                    log.debug("STOMP {} -> valid token but missing subject", cmd);
                }
            } else {
                log.debug("STOMP {} without/invalid token (public endpoints may allow)", cmd);
            }
        }
        return message;
    }

    private String extractBearer(StompHeaderAccessor accessor) {
        List<String> headers = accessor.getNativeHeader("Authorization");
        if (headers == null || headers.isEmpty()) {
            headers = accessor.getNativeHeader("authorization");
        }
        if (headers != null && !headers.isEmpty()) {
            String raw = headers.get(0);
            if (raw != null && raw.startsWith("Bearer ")) {
                return raw.substring(7);
            }
        }
        return null;
    }
}
