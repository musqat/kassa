package com.kassa.payment.reconcile

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant
import java.time.LocalDate

// 대조에서 나온 불일치 한 줄. 예를 들어 대행사에는 결제가 있는데 내 DB 에는 없는 경우
// 자동으로 고치지 않고 양쪽 값을 그대로 남긴다. 사람이 보고 판단한다
@Entity
class ReconcileDiff(
    targetDate: LocalDate,
    orderNo: String?,
    kind: DiffKind,
    gatewaySide: String?,
    localSide: String?,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var targetDate: LocalDate = targetDate
        protected set

    var orderNo: String? = orderNo
        protected set

    @Enumerated(EnumType.STRING)
    var kind: DiffKind = kind
        protected set

    // 금액·상태·거래 식별자를 문자열로 담는다
    var gatewaySide: String? = gatewaySide
        protected set

    var localSide: String? = localSide
        protected set

    var resolvedAt: Instant? = null
        protected set

    @Column(insertable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    /** 사람이 확인해 정리했다 */
    fun resolve(now: Instant) {
        resolvedAt = now
    }
}

enum class DiffKind {
    /** 대행사에는 있는데 내 쪽에 없다. 돈만 받고 주문이 없는 상태 */
    MISSING_LOCAL,

    /** 내 쪽에는 PAID 인데 대행사에 없다 */
    MISSING_GATEWAY,

    /** 금액이 다르다 */
    AMOUNT_MISMATCH,

    /** 상태가 다르다 */
    STATUS_MISMATCH,
}
