package com.kassa.support

import com.kassa.TestcontainersConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

// Testcontainers 로 PostgreSQL 을 띄운다
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
abstract class IntegrationTest
