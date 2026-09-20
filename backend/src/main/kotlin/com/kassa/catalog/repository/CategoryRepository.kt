package com.kassa.catalog.repository

import com.kassa.catalog.domain.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long>
