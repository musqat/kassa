package com.kassa.catalog.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant

@Entity
class Category(
    name: String,
    sortOrder: Int = 0,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
    var name: String = name
        protected set
    var sortOrder: Int = sortOrder
        protected set
    @Column(insertable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

}
