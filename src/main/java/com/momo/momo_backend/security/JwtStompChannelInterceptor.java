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
            String token = resolveBearer(getFirstNativeHeader(acc, "Authorization"));
            if (!StringUtils.hasText(token)) {
                token = resolveBearer(getFirstNativeHeader(acc, "authorization"));
            }
            if (!StringUtils.hasText(token)) {
                throw new IllegalArgumentException("Authorization header missing on STOMP CONNECT");
            }

            Principal principal;
            if (jwtTokenProvider.validateToken(token)) {
                var authentication = jwtTokenProvider.getAuthentication(token);
                if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails cud) {
                    String nameForUserDest = String.valueOf(cud.getNo()); // or cud.getId()
                    principal = new UsernamePasswordAuthenticationToken(
                            nameForUserDest, null, authentication.getAuthorities());
                } else {
                    String subject = jwtTokenProvider.getUserPk(token); // 구현에 맞게
                    principal = new UsernamePasswordAuthenticationToken(subject, null, List.of());
                }
            } else {
                throw new IllegalArgumentException("Invalid JWT on STOMP CONNECT");
            }
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
        if (header.startsWith("Bearer ")) return header.substring(7);
        return header;
    }
}
