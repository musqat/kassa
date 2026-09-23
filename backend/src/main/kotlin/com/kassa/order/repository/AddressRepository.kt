package com.kassa.order.repository

import com.kassa.order.domain.Address
import org.springframework.data.jpa.repository.JpaRepository

interface AddressRepository : JpaRepository<Address, Long> {

    fun findByUserIdAndIsDefaultTrue(userId: Long): Address?

    fun findAllByUserIdOrderByIdDesc(userId: Long): List<Address>
}
