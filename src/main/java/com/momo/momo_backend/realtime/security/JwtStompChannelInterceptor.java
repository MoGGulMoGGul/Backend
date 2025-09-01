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
            // 1) Bearer 추출
            String token = extractBearer(accessor); // raw JWT

            // 2) 디버그: CONNECT 프레임에 Authorization 헤더가 있었는지/토큰 앞 10자
            log.debug("STOMP {} rawHeaderExists={} tokenPrefix={}",
                    cmd,
                    accessor.getNativeHeader("Authorization") != null || accessor.getNativeHeader("authorization") != null,
                    token != null && token.length() >= 10 ? token.substring(0, 10) : "null");

            // 3) 디버그: 토큰에서 userNo 파싱 시도(유효성 검증 전/후 어디든 가능하지만 여기서 한 번 찍어보는게 원인 파악에 도움)
            if (token != null) {
                try {
                    Long parsedUserNo = jwtTokenProvider.getUserNo(token);
                    log.debug("STOMP {} parsed userNo={}", cmd, parsedUserNo);
                } catch (Exception e) {
                    log.debug("STOMP {} getUserNo threw: {}", cmd, e.toString());
                }
            }

            // 4) 기존 로직: 토큰 검증 및 Principal 세팅
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Long userNo = jwtTokenProvider.getUserNo(token);
                if (userNo != null) {
                    Authentication auth =
                            new UsernamePasswordAuthenticationToken(String.valueOf(userNo), null, List.of());
                    accessor.setUser(auth);
                    log.debug("STOMP {} -> authenticated userNo={}", cmd, userNo);
                } else {
                    String loginId = jwtTokenProvider.getUserIdFromToken(token);
                    if (loginId != null) {
                        accessor.setUser(new UsernamePasswordAuthenticationToken(loginId, null, List.of()));
                        log.warn("STOMP {} -> token has no userNo claim; set Principal to loginId={}, "
                                + "personal queue routing may not match (expects userNo string)", cmd, loginId);
                    } else {
                        log.debug("STOMP {} -> valid token but missing userNo/loginId", cmd);
                    }
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
