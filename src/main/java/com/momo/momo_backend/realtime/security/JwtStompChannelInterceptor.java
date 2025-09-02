package com.momo.momo_backend.realtime.security;

import com.momo.momo_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null) return message;

        StompCommand cmd = acc.getCommand();

        // ★ CONNECT 단계에서만 Principal 세팅 (SUBSCRIBE/SEND는 기존 principal 그대로 사용)
        if (StompCommand.CONNECT.equals(cmd)) {
            String token = extractBearer(acc); // raw JWT
            log.debug("STOMP CONNECT rawHeaderExists={} tokenPrefix={}",
                    acc.getNativeHeader("Authorization") != null || acc.getNativeHeader("authorization") != null,
                    token != null && token.length() >= 10 ? token.substring(0, 10) : "null");

            String principalName = null;
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                Long userNo = null;
                try {
                    userNo = jwtTokenProvider.getUserNo(token); // Long or null
                    log.debug("STOMP CONNECT parsed userNo={}", userNo);
                } catch (Exception e) {
                    log.debug("STOMP CONNECT getUserNo threw: {}", e.toString());
                }

                if (userNo != null) {
                    principalName = String.valueOf(userNo); // 개인 큐 라우팅 키는 "userNo" 문자열
                } else {
                    // 폴백: loginId가 있으면 세팅하되, /user 라우팅은 userNo를 기대하므로 경고
                    try {
                        String loginId = jwtTokenProvider.getUserIdFromToken(token);
                        if (StringUtils.hasText(loginId)) {
                            principalName = loginId;
                            log.warn("STOMP CONNECT -> token has no userNo; set principal to loginId='{}' "
                                    + "(personal-queue routing expects userNo string)", loginId);
                        }
                    } catch (Exception ignored) { }
                }
            } else {
                log.debug("STOMP CONNECT without/invalid token (public endpoints may allow)");
            }

            if (StringUtils.hasText(principalName)) {
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        principalName, "N/A", Collections.emptyList()
                );
                acc.setUser(auth);                                      // ★ STOMP 세션에 Principal 주입
                SecurityContextHolder.getContext().setAuthentication(auth); // (선택) SecurityContext에도 반영
                log.debug("STOMP CONNECT -> authenticated principal='{}'", principalName);
            }

            // ★★ 중요: 수정된 헤더로 새 메시지를 만들어 반환해야 acc 변경이 확실히 반영됨
            return MessageBuilder.createMessage(message.getPayload(), acc.getMessageHeaders());
        }

        // CONNECT 이외 : 그냥 통과 (principal은 CONNECT에서 이미 세팅됨)
        return message;
    }

    private String extractBearer(StompHeaderAccessor accessor) {
        List<String> headers = accessor.getNativeHeader("Authorization");
        if (headers == null || headers.isEmpty()) {
            headers = accessor.getNativeHeader("authorization");
        }
        if (headers != null && !headers.isEmpty()) {
            String raw = headers.get(0);
            if (raw != null && raw.startsWith("Bearer ")) return raw.substring(7);
        }
        return null;
    }
}
