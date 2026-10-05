package com.example.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey
    val productId: Int,
    val name: String,
    val price: Double,
    val originalPrice: Double?,
    val imageUrl: String?,
    val categoryName: String?,
    var quantity: Int
)
