package com.kassa.support

import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Table
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// 테스트마다 모든 테이블을 비운다. TRUNCATE CASCADE 라 FK 순서를 맞출 필요가 없다
@Component
class DatabaseCleaner {

    @PersistenceContext
    private lateinit var em: EntityManager

    private lateinit var tableNames: String

    // 엔티티 목록에서 테이블 이름을 뽑는다
    @PostConstruct
    fun collectTables() {
        tableNames = em.metamodel.entities
            .map { it.javaType.getAnnotation(Table::class.java)?.name ?: toSnakeCase(it.name) }
            .distinct()
            .joinToString(", ")
    }

    @Transactional
    fun clear() {
        em.createNativeQuery("TRUNCATE TABLE $tableNames RESTART IDENTITY CASCADE").executeUpdate()
    }

    private fun toSnakeCase(name: String) =
        name.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
}
