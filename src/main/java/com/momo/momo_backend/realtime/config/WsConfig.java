package com.momo.momo_backend.realtime.config;

import com.momo.momo_backend.realtime.RealtimeProperties;
import com.momo.momo_backend.realtime.security.JwtStompChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(RealtimeProperties.class)
@RequiredArgsConstructor
public class WsConfig implements WebSocketMessageBrokerConfigurer {

    private final RealtimeProperties props;
    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // props.getEndpoint() == "/ws" 로 가정
        var ep = registry.addEndpoint(props.getEndpoint());

        // ✅ WS 핸드셰이크 CORS도 프론트 오리진 허용
        ep.setAllowedOriginPatterns(allowedWsOrigins());
        // 필요 시 SockJS 사용
        // ep.withSockJS();
    }

    private String[] allowedWsOrigins() {
        return new String[]{
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://moggulmoggul-frontend.s3-website.ap-northeast-2.amazonaws.com",
                "https://moggulmoggul-frontend.s3-website.ap-northeast-2.amazonaws.com"
                // CloudFront/커스텀 도메인 추가 가능
        };
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes(props.getAppPrefix());         // 예: "/app"
        registry.setUserDestinationPrefix(props.getUserDestinationPrefix());      // 예: "/user"
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }
}
