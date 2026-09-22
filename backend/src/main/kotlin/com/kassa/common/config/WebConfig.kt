package com.kassa.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 허용 출처는 설정으로 받는다. 배포에서는 환경 변수 APP_CORS_ORIGINS 로 넣는다.
 */
@ConfigurationProperties("app.cors")
data class CorsProperties(
    val origins: List<String> = emptyList(),
)

@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class WebConfig(
    private val cors: CorsProperties,
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(*cors.origins.toTypedArray())
            .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
            .exposedHeaders("X-Request-Id")
    }
}
