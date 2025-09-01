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
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(RealtimeProperties.class)
@RequiredArgsConstructor
public class WsConfig implements WebSocketMessageBrokerConfigurer {

    private final RealtimeProperties props; // 엔드포인트 경로 등은 그대로 props 사용
    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var ep = registry.addEndpoint(props.getEndpoint());
        ep.setAllowedOrigins(allowedOrigins); // 여기만 교체!
        // ep.withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes(props.getAppPrefix());
        registry.setUserDestinationPrefix(props.getUserDestinationPrefix());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }
}
