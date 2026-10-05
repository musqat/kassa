package com.kassa.payment.gateway

import java.net.http.HttpClient
import java.time.Duration
import java.util.Base64
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient

@ConfigurationProperties("app.payment.toss")
data class TossProperties(
    val baseUrl: String,
    val secretKey: String,
    val connectTimeout: Duration,
    val readTimeout: Duration,
    /** 거래 조회는 60초까지 걸린다고 문서에 적혀 있다 */
    val transactionsTimeout: Duration,
)

@Configuration
@EnableConfigurationProperties(TossProperties::class)
@ConditionalOnProperty(name = ["app.payment.gateway"], havingValue = "toss")
class TossConfig {

    @Bean
    fun tossRestClient(builder: RestClient.Builder, properties: TossProperties): RestClient =
        client(builder, properties, properties.readTimeout)

    // 거래 조회만 따로 둔다. 승인에까지 60초를 주면 결제창이 그만큼 멈춘다
    @Bean
    fun tossTransactionsRestClient(builder: RestClient.Builder, properties: TossProperties): RestClient =
        client(builder, properties, properties.transactionsTimeout)

    // 주입받은 빌더를 쓴다. 직접 만들면 모르는 필드에서 실패하는 기본 변환기가 붙는다
    private fun client(
        builder: RestClient.Builder,
        properties: TossProperties,
        readTimeout: Duration,
    ): RestClient {
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(properties.connectTimeout)
            .build()

        val factory = JdkClientHttpRequestFactory(httpClient)
        factory.setReadTimeout(readTimeout)

        // 시크릿 키가 사용자 이름이고 비밀번호는 없다. 콜론을 빼면 인증이 실패한다
        val credentials = Base64.getEncoder().encodeToString("${properties.secretKey}:".toByteArray())

        return builder.clone()
            .baseUrl(properties.baseUrl)
            .requestFactory(factory)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic $credentials")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }
}
