package com.kassa

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	fun postgresContainer(): PostgreSQLContainer {
		// 로컬 docker-compose·배포(Neon)와 같은 메이저로 맞춘다.
		// latest 를 쓰면 시점에 따라 테스트 대상 DB 가 달라진다.
		return PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
	}

}
