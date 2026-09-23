package com.kassa.catalog

import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.domain.ProductStatus
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.cart.repository.CartItemRepository
import com.kassa.support.IntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class ProductControllerTest : IntegrationTest() {

    @Autowired
    private lateinit var cartItemRepository: CartItemRepository

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var productRepository: ProductRepository


    private lateinit var categoryA: Category
    private lateinit var categoryB: Category

    private lateinit var water: Product
    private lateinit var coffee: Product
    private lateinit var chips: Product
    private lateinit var hidden: Product

    @BeforeEach
    fun setUp() {
        cartItemRepository.deleteAll()
        productRepository.deleteAll()
        categoryRepository.deleteAll()

        categoryA = categoryRepository.save(Category("음료"))
        categoryB = categoryRepository.save(Category("과자"))

        water = productRepository.save(Product(categoryA, "생수", 1000))
        coffee = productRepository.save(Product(categoryA, "커피", 3000))
        chips = productRepository.save(Product(categoryB, "감자칩", 2000, status = ProductStatus.SOLD_OUT))
        hidden = productRepository.save(Product(categoryA, "숨김상품", 5000, status = ProductStatus.HIDDEN))
    }

    @Test
    fun `목록은 HIDDEN을 제외한다`() {
        mockMvc.get("/api/products")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(3) }
                jsonPath("$[?(@.name == '숨김상품')]") { isEmpty() }
            }
    }

    @Test
    fun `카테고리로 거른다`() {
        mockMvc.get("/api/products") {
            param("categoryId", categoryA.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }   // 생수, 커피
        }
    }

    @Test
    fun `단건 조회는 이름과 가격을 준다`() {
        mockMvc.get("/api/products/${water.id}")
            .andExpect {
                status { isOk() }
                jsonPath("$.name") { value("생수") }
                jsonPath("$.price") { value(1000) }
            }
    }

    @Test
    fun `없는 상품은 404와 CATALOG_001을 준다`() {
        mockMvc.get("/api/products/99999")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("CATALOG_001") }
            }
    }

    @Test
    fun `HIDDEN상품은 단건 조회도 404다`() {
        mockMvc.get("/api/products/${hidden.id}")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("CATALOG_001") }
            }
    }

    @Test
    fun `응답에 재고를 노출하지 않는다`() {
        mockMvc.get("/api/products")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].stock") { doesNotExist() }
                jsonPath("$[0].reservedStock") { doesNotExist() }
            }
    }
}
