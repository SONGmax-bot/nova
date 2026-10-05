package com.example.data.repository

import com.example.data.local.database.CartDao
import com.example.data.local.database.CartItemEntity
import com.example.data.model.CategoryDto
import com.example.data.model.CreateOrderRequest
import com.example.data.model.HealthData
import com.example.data.model.OrderDto
import com.example.data.model.OrderItemRequest
import com.example.data.model.ProductDto
import com.example.data.remote.ApiClient
import kotlinx.coroutines.flow.Flow

class StoreRepository(
    private val apiClient: ApiClient,
    private val cartDao: CartDao
) {

    val cartItemsFlow: Flow<List<CartItemEntity>> = cartDao.getAllCartItems()
    val totalCartCountFlow: Flow<Int?> = cartDao.getTotalCartCount()

    suspend fun getHealth(): Result<HealthData> {
        return try {
            val response = apiClient.getService().getHealth()
            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success(response.body()!!.data ?: HealthData())
            } else {
                Result.failure(Exception("الخادم غير متاح حالياً"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategories(): Result<List<CategoryDto>> {
        return try {
            val response = apiClient.getService().getCategories()
            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.error ?: "فشل في تحميل الأقسام"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProducts(
        categoryId: Int? = null,
        query: String? = null,
        featured: Int? = null
    ): Result<List<ProductDto>> {
        return try {
            val response = apiClient.getService().getProducts(
                categoryId = if (categoryId != null && categoryId > 0) categoryId else null,
                query = query?.ifBlank { null },
                featured = featured
            )
            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.error ?: "فشل في تحميل المنتجات"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductDetails(id: Int): Result<ProductDto> {
        return try {
            val response = apiClient.getService().getProductDetails(id)
            if (response.isSuccessful && response.body()?.ok == true && response.body()!!.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.error ?: "المنتج غير متوفر"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToCart(product: ProductDto, quantity: Int = 1) {
        val existing = cartDao.getCartItemById(product.id)
        if (existing != null) {
            cartDao.update(existing.copy(quantity = existing.quantity + quantity))
        } else {
            cartDao.insertOrUpdate(
                CartItemEntity(
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    originalPrice = product.originalPrice,
                    imageUrl = product.imageUrl,
                    categoryName = product.categoryName,
                    quantity = quantity
                )
            )
        }
    }

    suspend fun updateCartQuantity(productId: Int, delta: Int) {
        val existing = cartDao.getCartItemById(productId) ?: return
        val newQuantity = existing.quantity + delta
        if (newQuantity <= 0) {
            cartDao.delete(existing)
        } else {
            cartDao.update(existing.copy(quantity = newQuantity))
        }
    }

    suspend fun removeFromCart(productId: Int) {
        cartDao.deleteByProductId(productId)
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }

    suspend fun createOrder(shippingAddress: String, items: List<CartItemEntity>): Result<String> {
        return try {
            val request = CreateOrderRequest(
                shippingAddress = shippingAddress,
                items = items.map {
                    OrderItemRequest(
                        productId = it.productId,
                        quantity = it.quantity,
                        price = it.price
                    )
                }
            )

            val response = apiClient.getService().createOrder(request)
            if (response.isSuccessful && response.body()?.ok == true) {
                cartDao.clearCart()
                Result.success(response.body()!!.message ?: "تم إنشاء الطلب بنجاح")
            } else {
                Result.failure(Exception(response.body()?.error ?: "فشل في إتمام الطلب"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrders(): Result<List<OrderDto>> {
        return try {
            val response = apiClient.getService().getOrders()
            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.error ?: "فشل في جلب الطلبات"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
