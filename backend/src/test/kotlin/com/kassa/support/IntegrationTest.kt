package com.kassa.support

import com.kassa.TestcontainersConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * 통합 테스트 공통 설정.
 *
 * Testcontainers 로 실제 PostgreSQL 을 띄운다. H2 로는 SELECT FOR UPDATE 와
 * 부분 유니크 인덱스를 검증할 수 없어 M3 의 동시성 테스트가 의미를 잃는다.
 *
 * 스프링이 테스트 컨텍스트를 캐시하므로 컨테이너는 설정이 같은 테스트끼리 공유된다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
abstract class IntegrationTest
