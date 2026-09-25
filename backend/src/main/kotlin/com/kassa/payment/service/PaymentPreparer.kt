package com.kassa.payment.service

import com.kassa.payment.gateway.PaymentGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(PaymentPreparer::class.java)

// 결제창을 열기 전에 금액을 등록한다. 주문 트랜잭션이 커밋된 뒤에 부른다
@Component
class PaymentPreparer(private val gateway: PaymentGateway) {

    /** 실패해도 던지지 않는다. 주문은 이미 저장됐고 결제창만 못 연다 */
    fun prepare(orderNo: String, amount: Long): Boolean {
        return try {
            gateway.preRegister(orderNo, amount)
            true
        } catch (e: Exception) {
            log.warn("결제 금액 사전 등록 실패: {}", orderNo, e)
            false
        }
    }
}
