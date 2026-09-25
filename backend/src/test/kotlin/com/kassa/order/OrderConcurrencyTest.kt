package com.kassa.order

import com.kassa.cart.domain.CartItem
import com.kassa.cart.repository.CartItemRepository
import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.order.dto.PlaceOrderRequest
import com.kassa.order.repository.AddressRepository
import com.kassa.order.repository.OrderRepository
import com.kassa.order.service.OrderService
import com.kassa.support.IntegrationTest
import com.kassa.user.domain.User
import com.kassa.user.repository.EmailTokenRepository
import com.kassa.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OrderConcurrencyTest : IntegrationTest() {

    @Autowired
    private lateinit var orderService: OrderService
    @Autowired
    private lateinit var orderRepository: OrderRepository
    @Autowired
    private lateinit var addressRepository: AddressRepository
    @Autowired
    private lateinit var cartItemRepository: CartItemRepository
    @Autowired
    private lateinit var productRepository: ProductRepository
    @Autowired
    private lateinit var categoryRepository: CategoryRepository
    @Autowired
    private lateinit var emailTokenRepository: EmailTokenRepository
    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var product: Product

    @BeforeEach
    fun setUp() {
        orderRepository.deleteAll()
        addressRepository.deleteAll()
        cartItemRepository.deleteAll()
        productRepository.deleteAll()
        categoryRepository.deleteAll()
        emailTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    private fun givenProduct(stock: Int) {
        val category = categoryRepository.save(Category("음료"))
        product = productRepository.save(Product(category, "한정판 머그컵", 19_000, stock = stock))
    }

    // 회원과 장바구니를 사람 수만큼 만든다. 각자 같은 상품을 1개씩 담는다
    private fun givenBuyers(count: Int): List<Long> =
        (1..count).map { index ->
            val user = userRepository.save(
                User("buyer$index", "buyer$index@example.com", "hash", "구매자$index")
                    .apply { verifyEmail(Instant.now()) },
            )
            cartItemRepository.save(CartItem(user.id!!, product, 1))
            user.id!!
        }

    private fun request() = PlaceOrderRequest(
        receiver = "홍길동",
        phone = "010-1234-5678",
        zipcode = "06236",
        addr1 = "서울 강남구 테헤란로 1",
    )

    /**
     * 모두 같은 순간에 주문하게 만든다.
     * 성공한 건수와 재고 부족으로 실패한 건수를 돌려준다.
     */
    private fun placeAllAtOnce(userIds: List<Long>): Result {
        val start = CountDownLatch(1)
        val done = CountDownLatch(userIds.size)
        val pool = Executors.newFixedThreadPool(userIds.size)
        var success = 0
        var outOfStock = 0
        val lock = Any()

        userIds.forEach { userId ->
            pool.submit {
                start.await()
                try {
                    orderService.place(userId, request())
                    synchronized(lock) { success++ }
                } catch (e: BusinessException) {
                    if (e.errorCode == ErrorCode.OUT_OF_STOCK) {
                        synchronized(lock) { outOfStock++ }
                    }
                } catch (e: Exception) {
                    // 잠금 경합으로 난 예외도 실패로 센다
                    synchronized(lock) { outOfStock++ }
                } finally {
                    done.countDown()
                }
            }
        }

        start.countDown()
        done.await(30, TimeUnit.SECONDS)
        pool.shutdown()

        val saved = productRepository.findById(product.id!!).get()
        return Result(success, outOfStock, saved.stock, saved.reservedStock)
    }

    data class Result(
        val success: Int,
        val outOfStock: Int,
        val stock: Int,
        val reservedStock: Int,
    )

    @Test
    fun `재고 1개에 두 명이 동시에 주문하면 한 명만 성공한다`() {
        givenProduct(stock = 1)
        val buyers = givenBuyers(2)

        val result = placeAllAtOnce(buyers)

        assertThat(result.success).isEqualTo(1)
        assertThat(result.outOfStock).isEqualTo(1)
        assertThat(result.reservedStock).isEqualTo(1)
        assertThat(orderRepository.count()).isEqualTo(1)
    }

    @Test
    fun `재고 10개에 20명이 동시에 주문하면 10명만 성공한다`() {
        givenProduct(stock = 10)
        val buyers = givenBuyers(20)

        val result = placeAllAtOnce(buyers)

        assertThat(result.success).isEqualTo(10)
        assertThat(result.outOfStock).isEqualTo(10)
        assertThat(result.reservedStock).isEqualTo(10)
        assertThat(result.stock - result.reservedStock).isGreaterThanOrEqualTo(0)
    }
}
