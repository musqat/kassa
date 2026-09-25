package com.kassa.payment.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.s
import java.time.Duration
import java.time.Instant

class SagaStateTest {

    private val now: Instant = Instant.parse("2026-09-26T10:00:00Z")
    private val later: Instant = now.plus(Duration.ofMinutes(1))

    private fun instance() = SagaInstance("ORDER_PAYMENT", "20260926-ABCD1234")

    private fun step() = SagaStep(sagaInstanceId = 1, stepName = "APPROVE_PAYMENT")

    private fun doneStep() = step().apply {
        begin(now)
        done("42")
    }

    @Test
    fun `사가는 RUNNING 으로 시작한다`() {
        val saga = instance()

        assertThat(saga.status).isEqualTo(SagaStatus.RUNNING)
        assertThat(saga.currentStep).isNull()
    }

    @Test
    fun `단계에 들어가면 updatedAt 이 갱신된다`() {
        val saga = instance()
        saga.enterStep("APPROVE_PAYMENT", later)

        assertThat(saga.currentStep).isEqualTo("APPROVE_PAYMENT")
        assertThat(saga.updatedAt).isEqualTo(later)
    }

    @Test
    fun `모든 단계를 마치면 COMPLETED`() {
        val saga = instance()
        saga.complete(later)

        assertThat(saga.status).isEqualTo(SagaStatus.COMPLETED)
        assertThat(saga.updatedAt).isEqualTo(later)
    }

    @Test
    fun `되돌리기를 시작하면 COMPENSATING`() {
        val saga = instance()
        saga.startCompensating(later)

        assertThat(saga.status).isEqualTo(SagaStatus.COMPENSATING)
        assertThat(saga.updatedAt).isEqualTo(later)
    }

    @Test
    fun `되돌리기를 마치면 FAILED`() {
        val saga = instance()
        saga.startCompensating(now)
        saga.compensated(later)

        assertThat(saga.status).isEqualTo(SagaStatus.FAILED)
        assertThat(saga.updatedAt).isEqualTo(later)
    }

    @Test
    fun `되돌리기가 거듭 실패하면 NEEDS_ATTENTION`() {
        val saga = instance()
        saga.startCompensating(now)
        saga.needsAttention(later)

        assertThat(saga.status).isEqualTo(SagaStatus.NEEDS_ATTENTION)
    }

    @Test
    fun `되돌리기 중이 아닌 사가는 마칠 수 없다`() {
        val saga = instance()

        assertThatThrownBy { saga.compensated(later) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("RUNNING")
    }

    @Test
    fun `이미 끝난 사가를 다시 되돌릴 수 없다`() {
        val saga = instance()
        saga.complete(now)

        assertThatThrownBy { saga.startCompensating(later) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("COMPLETED")
    }

    @Test
    fun `단계는 PENDING 으로 시작한다`() {
        val step = step()

        assertThat(step.status).isEqualTo(StepStatus.PENDING)
        assertThat(step.attemptCount).isEqualTo(0)
    }

    @Test
    fun `실행할 때마다 시도 횟수가 오른다`() {
        val step = step()
        step.begin(now)
        step.fail("타임아웃")
        step.begin(later)

        assertThat(step.attemptCount).isEqualTo(2)
        assertThat(step.executedAt).isEqualTo(later)
    }

    @Test
    fun `멱등키는 한 번 잡으면 바뀌지 않는다`() {
        val step = step()
        step.claimKey("key-1")
        step.claimKey("key-2")

        assertThat(step.idempotencyKey).isEqualTo("key-1")
    }

    @Test
    fun `재시도로 성공하면 앞선 실패 메시지가 지워진다`() {
        val step = step()
        step.begin(now)
        step.fail("타임아웃")
        step.begin(later)
        step.done("42")

        assertThat(step.status).isEqualTo(StepStatus.DONE)
        assertThat(step.payload).isEqualTo("42")
        assertThat(step.error).isNull()
    }

    @Test
    fun `실패 사유가 길면 잘라서 담는다`() {
        val step = step()
        step.fail("긴".repeat(300))

        assertThat(step.error).hasSize(255)
    }

    @Test
    fun `마친 단계는 되돌릴 수 있다`() {
        val step = doneStep()
        step.compensated()

        assertThat(step.status).isEqualTo(StepStatus.COMPENSATED)
    }

    @Test
    fun `마치지 않은 단계는 되돌릴 수 없다`() {
        assertThatThrownBy { step().compensated() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("PENDING")
    }
}
