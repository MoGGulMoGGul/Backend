package com.momo.momo_backend.realtime.config;

import com.momo.momo_backend.realtime.RealtimeProperties;
import com.momo.momo_backend.realtime.security.JwtStompChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(RealtimeProperties.class)
@RequiredArgsConstructor
public class WsConfig implements WebSocketMessageBrokerConfigurer {

    private final RealtimeProperties props;
    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    // relay or simple (default: simple)
    @Value("${realtime.broker:simple}")
    private String brokerMode;

    // Relay 모드에서만 사용
    @Value("${stomp.relay.host:localhost}")
    private String relayHost;

    @Value("${stomp.relay.port:61613}")
    private int relayPort;

    @Value("${stomp.relay.client-login:guest}")
    private String relayClientLogin;

    @Value("${stomp.relay.client-passcode:guest}")
    private String relayClientPasscode;

    @Value("${stomp.relay.system-login:guest}")
    private String relaySystemLogin;

    @Value("${stomp.relay.system-passcode:guest}")
    private String relaySystemPasscode;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Origin 비교는 문자열 매칭이라, 뒤에 슬래시 없는 도메인만 넣어둘 것
        registry.addEndpoint(props.getEndpoint())
                .setAllowedOriginPatterns(allowedOrigins); // 패턴 허용(와일드카드 등)
        // .withSockJS(); // 필요 시
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }

    @Bean
    public TaskScheduler brokerTaskScheduler() {
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(2);
        ts.setThreadNamePrefix("ws-broker-");
        ts.initialize();
        return ts;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry r) {
        if ("relay".equalsIgnoreCase(brokerMode)) {
            r.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setClientLogin(relayClientLogin)
                    .setClientPasscode(relayClientPasscode)
                    .setSystemLogin(relaySystemLogin)
                    .setSystemPasscode(relaySystemPasscode)
                    .setVirtualHost("/"); // 필요시 변경
        } else {
            r.enableSimpleBroker("/topic", "/queue")
                    .setTaskScheduler(brokerTaskScheduler())
                    .setHeartbeatValue(new long[]{10000, 10000}); // 10s/10s
        }
        r.setApplicationDestinationPrefixes(props.getAppPrefix());
        r.setUserDestinationPrefix(props.getUserDestinationPrefix());
    }
}
