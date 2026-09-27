package com.kassa.payment.reconcile

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.Instant
import java.time.LocalDate
import org.springframework.context.annotation.Profile
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// 대조 결과를 보고 다시 돌리는 창구
// 역할 구분(ROLE_ADMIN)이 아직 없어 로컬·테스트 프로파일에서만 빈이 올라온다
@Tag(name = "결제 기록 대조")
@RestController
@RequestMapping("/api/admin/reconcile")
@Profile("local", "test")
class ReconcileController(
    private val reconcileService: ReconcileService,
    private val diffRepository: ReconcileDiffRepository,
) {

    @Operation(summary = "불일치 목록", description = "그날 대조에서 나온 불일치를 본다")
    @GetMapping
    fun findDiffs(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): List<DiffResponse> =
        diffRepository.findAllByTargetDateOrderByIdAsc(date).map { DiffResponse.of(it) }

    @Operation(summary = "대조 실행", description = "그날치를 다시 맞춰 본다. 이미 남긴 건은 다시 넣지 않는다")
    @PostMapping
    fun run(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): RunResponse = RunResponse(date, reconcileService.reconcile(date))
}

data class DiffResponse(
    val id: Long,
    val targetDate: LocalDate,
    val orderNo: String?,
    val kind: DiffKind,
    val gatewaySide: String?,
    val localSide: String?,
    val resolvedAt: Instant?,
) {
    companion object {
        fun of(diff: ReconcileDiff) = DiffResponse(
            id = diff.id!!,
            targetDate = diff.targetDate,
            orderNo = diff.orderNo,
            kind = diff.kind,
            gatewaySide = diff.gatewaySide,
            localSide = diff.localSide,
            resolvedAt = diff.resolvedAt,
        )
    }
}

data class RunResponse(val targetDate: LocalDate, val found: Int)
