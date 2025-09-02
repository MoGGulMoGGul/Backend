package com.momo.momo_backend.realtime.config;

import com.momo.momo_backend.realtime.RealtimeProperties;
import com.momo.momo_backend.realtime.security.JwtStompChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.beans.factory.annotation.Value;

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
        ep.setAllowedOrigins(allowedOrigins);
        // ep.withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }

    @Bean
    public TaskScheduler brokerTaskScheduler() {
        return new ThreadPoolTaskScheduler(); // poolSize/treadNamePrefix 선택 설정
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry r) {
        r.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(brokerTaskScheduler())
                .setHeartbeatValue(new long[]{10000, 10000}); // 10s/10s
        r.setApplicationDestinationPrefixes(props.getAppPrefix());
        r.setUserDestinationPrefix(props.getUserDestinationPrefix());
    }

}
