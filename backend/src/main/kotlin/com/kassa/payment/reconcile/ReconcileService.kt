package com.kassa.payment.reconcile

import com.kassa.order.repository.OrderRepository
import com.kassa.payment.domain.Payment
import com.kassa.payment.domain.PaymentStatus
import com.kassa.payment.gateway.GatewayPayment
import com.kassa.payment.gateway.PaymentGateway
import com.kassa.payment.repository.PaymentRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = LoggerFactory.getLogger(ReconcileService::class.java)

// 대행사에 남은 결제와 내 DB 의 결제를 하루치씩 대조한다
// 회계에서 두 장부를 맞춰 보는 작업을 reconciliation 이라 불러서 이름을 그렇게 뒀다
// 승인 응답·웹훅·확인 스케줄러가 모두 놓친 건이 여기서 드러난다
@Service
class ReconcileService(
    private val gateway: PaymentGateway,
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val diffRepository: ReconcileDiffRepository,
    private val clock: Clock,
) {

    // 리턴은 불일치 건수
    // 자동으로 고치지 않는다. 돈이 걸린 자리를 배치가 임의로 맞추면 틀렸을 때 되돌릴 근거가 없다
    fun reconcile(targetDate: LocalDate): Int {
        val from = targetDate.atStartOfDay(KST).toInstant()
        val to = targetDate.plusDays(1).atStartOfDay(KST).toInstant()

        val gatewayPayments = gateway.findPayments(from, to).associateBy { it.orderNo }
        // 그날 승인된 내 결제를 주문번호로 묶는다. 주문이 없는 결제는 뺀다
        val localPaid = paymentRepository.findAllByStatusAndApprovedAtBetween(PaymentStatus.PAID, from, to)
            .mapNotNull { payment ->
                orderRepository.findById(payment.orderId).orElse(null)?.let { it.orderNo to payment }
            }
            .toMap()

        var found = 0

        for (orderNo in gatewayPayments.keys - localPaid.keys) {
            val side = describe(gatewayPayments.getValue(orderNo))
            if (record(targetDate, orderNo, DiffKind.MISSING_LOCAL, side, null)) found++
        }

        for (orderNo in localPaid.keys - gatewayPayments.keys) {
            val side = describe(localPaid.getValue(orderNo))
            if (record(targetDate, orderNo, DiffKind.MISSING_GATEWAY, null, side)) found++
        }

        for (orderNo in gatewayPayments.keys intersect localPaid.keys) {
            val remote = gatewayPayments.getValue(orderNo)
            val local = localPaid.getValue(orderNo)
            if (remote.amount == local.amount) continue

            if (record(targetDate, orderNo, DiffKind.AMOUNT_MISMATCH, describe(remote), describe(local))) found++
        }

        return found
    }

    private fun describe(payment: GatewayPayment) =
        "amount=${payment.amount}, status=${payment.status}, tx=${payment.transactionId}"

    private fun describe(payment: Payment) =
        "amount=${payment.amount}, status=${payment.status}, tx=${payment.transactionId}"

    // 같은 날짜·주문번호·종류가 이미 있으면 넣지 않는다. 리턴은 새로 넣었는지
    @Transactional
    fun record(
        targetDate: LocalDate,
        orderNo: String?,
        kind: DiffKind,
        gatewaySide: String?,
        localSide: String?,
    ): Boolean {
        if (diffRepository.existsByTargetDateAndOrderNoAndKind(targetDate, orderNo, kind)) {
            return false
        }

        // 돈만 받고 주문이 없는 상태다. 사람이 바로 봐야 한다
        if (kind == DiffKind.MISSING_LOCAL) {
            log.error("대행사에만 있는 결제: {} {}", targetDate, orderNo)
        }

        diffRepository.save(ReconcileDiff(targetDate, orderNo, kind, gatewaySide, localSide))
        return true
    }

    companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
