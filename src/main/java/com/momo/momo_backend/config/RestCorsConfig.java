package com.momo.momo_backend.config;

import com.momo.momo_backend.realtime.RealtimeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(RealtimeProperties.class)
@RequiredArgsConstructor
public class RestCorsConfig implements WebMvcConfigurer {

    private final RealtimeProperties props;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // REST API 엔드포인트
        registry.addMapping("/api/**")
                .allowedOriginPatterns(props.getCorsAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // (선택) Swagger UI 접근 열기
        registry.addMapping("/v3/api-docs/**")
                .allowedOriginPatterns(props.getCorsAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET","OPTIONS");

        registry.addMapping("/swagger-ui/**")
                .allowedOriginPatterns(props.getCorsAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET","OPTIONS");
    }
}
