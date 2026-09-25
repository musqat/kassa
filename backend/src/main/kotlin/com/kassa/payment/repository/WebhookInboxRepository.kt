package com.kassa.payment.repository

import com.kassa.payment.domain.InboxStatus
import com.kassa.payment.domain.WebhookInbox
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface WebhookInboxRepository : JpaRepository<WebhookInbox, Long> {

    fun existsByEventId(eventId: String): Boolean

    // 아직 처리하지 않은 웹훅. 다른 워커가 잡은 행은 건너뛴다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select w from WebhookInbox w where w.status = :status order by w.id")
    fun findUnprocessed(
        @Param("status") status: InboxStatus,
        limit: Limit,
    ): List<WebhookInbox>
}
