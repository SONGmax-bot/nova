package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProductDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "original_price") val originalPrice: Double? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "stock") val stock: Int = 0,
    @Json(name = "rating") val rating: Double = 4.8,
    @Json(name = "is_featured") val isFeatured: Int = 0,
    @Json(name = "category_id") val categoryId: Int? = null,
    @Json(name = "category_name") val categoryName: String? = null
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "icon_url") val iconUrl: String? = null,
    @Json(name = "products_count") val productsCount: Int? = 0
)

@JsonClass(generateAdapter = true)
data class OrderDto(
    @Json(name = "id") val id: Int,
    @Json(name = "total_amount") val totalAmount: Double = 0.0,
    @Json(name = "status") val status: String = "pending",
    @Json(name = "shipping_address") val shippingAddress: String? = null,
    @Json(name = "payment_method") val paymentMethod: String? = null,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "items_count") val itemsCount: Int? = 1
)

@JsonClass(generateAdapter = true)
data class CreateOrderRequest(
    @Json(name = "shipping_address") val shippingAddress: String,
    @Json(name = "items") val items: List<OrderItemRequest>
)

@JsonClass(generateAdapter = true)
data class OrderItemRequest(
    @Json(name = "product_id") val productId: Int,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "price") val price: Double
)

@JsonClass(generateAdapter = true)
data class HealthData(
    @Json(name = "status") val status: String = "online",
    @Json(name = "store_name") val storeName: String? = "NOVA STORE",
    @Json(name = "version") val version: String? = null,
    @Json(name = "database") val database: String? = "connected",
    @Json(name = "timestamp") val timestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class SettingsData(
    @Json(name = "store_name") val storeName: String? = "NOVA STORE",
    @Json(name = "currency") val currency: String? = "USD",
    @Json(name = "currency_symbol") val currencySymbol: String? = "$",
    @Json(name = "shipping_fee") val shippingFee: Double? = 15.0
)
