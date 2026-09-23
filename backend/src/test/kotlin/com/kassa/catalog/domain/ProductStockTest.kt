package com.kassa.catalog.domain

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ProductStockTest {

    private fun product(stock: Int = 10) = Product(Category("음료"), "생수", 4_800, stock = stock)

    @Test
    fun `선점하면 판매 가능 수량만 줄어든다`() {
        val product = product()

        product.reserve(2)

        assertThat(product.stock).isEqualTo(10)
        assertThat(product.reservedStock).isEqualTo(2)
        assertThat(product.availableStock()).isEqualTo(8)
    }

    @Test
    fun `판매 가능 수량보다 많이 선점하면 ORDER_001`() {
        val product = product()
        product.reserve(8)

        assertThatThrownBy { product.reserve(3) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.OUT_OF_STOCK)
    }

    @Test
    fun `판매 가능 수량만큼은 선점된다`() {
        val product = product()

        product.reserve(10)

        assertThat(product.availableStock()).isEqualTo(0)
    }

    @Test
    fun `확정하면 재고와 선점이 함께 줄어든다`() {
        val product = product()
        product.reserve(2)

        product.confirmReservation(2)

        assertThat(product.stock).isEqualTo(8)
        assertThat(product.reservedStock).isEqualTo(0)
        assertThat(product.availableStock()).isEqualTo(8)
    }

    @Test
    fun `해제하면 선점만 돌아온다`() {
        val product = product()
        product.reserve(2)

        product.releaseReservation(2)

        assertThat(product.stock).isEqualTo(10)
        assertThat(product.reservedStock).isEqualTo(0)
    }

    @Test
    fun `선점하지 않은 수량은 해제할 수 없다`() {
        val product = product()
        product.reserve(1)

        assertThatThrownBy { product.releaseReservation(2) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `선점하지 않은 수량은 확정할 수 없다`() {
        val product = product()

        assertThatThrownBy { product.confirmReservation(1) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `여러 번 선점하고 일부만 확정해도 수량이 맞는다`() {
        val product = product()
        product.reserve(3)
        product.reserve(2)

        product.confirmReservation(3)

        assertThat(product.stock).isEqualTo(7)
        assertThat(product.reservedStock).isEqualTo(2)
        assertThat(product.availableStock()).isEqualTo(5)
    }
}
