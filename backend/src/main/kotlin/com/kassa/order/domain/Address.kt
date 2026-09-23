package com.kassa.order.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "address")
class Address(
    userId: Long,
    shipping: ShippingInfo,
    isDefault: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var userId: Long = userId
        protected set

    var receiver: String = shipping.receiver
        protected set

    var phoneEnc: String = shipping.phoneEnc
        protected set

    var zipcode: String = shipping.zipcode
        protected set

    var addr1: String = shipping.addr1
        protected set

    var addr2: String? = shipping.addr2
        protected set

    var isDefault: Boolean = isDefault
        protected set

    @Column(insertable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    fun unsetDefault() {
        isDefault = false
    }

    fun toShippingInfo() = ShippingInfo(receiver, phoneEnc, zipcode, addr1, addr2)
}
