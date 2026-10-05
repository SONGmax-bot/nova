package com.example.data.remote

import com.example.data.model.ApiResponse
import com.example.data.model.AuthData
import com.example.data.model.CategoryDto
import com.example.data.model.ChangePasswordRequest
import com.example.data.model.CreateOrderRequest
import com.example.data.model.HealthData
import com.example.data.model.LoginRequest
import com.example.data.model.OrderDto
import com.example.data.model.ProductDto
import com.example.data.model.ProfileUpdateRequest
import com.example.data.model.RegisterRequest
import com.example.data.model.SettingsData
import com.example.data.model.UserWrapper
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface NovaApiService {

    @GET("api.php?endpoint=health")
    suspend fun getHealth(): Response<ApiResponse<HealthData>>

    @GET("api.php?endpoint=settings")
    suspend fun getSettings(): Response<ApiResponse<SettingsData>>

    @GET("api.php?endpoint=categories")
    suspend fun getCategories(): Response<ApiResponse<List<CategoryDto>>>

    @GET("api.php?endpoint=products")
    suspend fun getProducts(
        @Query("category_id") categoryId: Int? = null,
        @Query("q") query: String? = null,
        @Query("featured") featured: Int? = null
    ): Response<ApiResponse<List<ProductDto>>>

    @GET("api.php?endpoint=product")
    suspend fun getProductDetails(
        @Query("id") productId: Int
    ): Response<ApiResponse<ProductDto>>

    // ====================================================
    // USER AUTHENTICATION & PROFILE ENDPOINTS
    // ====================================================

    @POST("api.php?endpoint=register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<AuthData>>

    @POST("api.php?endpoint=login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthData>>

    @GET("api.php?endpoint=me")
    suspend fun getMe(): Response<ApiResponse<UserWrapper>>

    @POST("api.php?endpoint=logout")
    suspend fun logout(): Response<ApiResponse<Map<String, Any>>>

    @POST("api.php?endpoint=profile_update")
    suspend fun updateProfile(
        @Body request: ProfileUpdateRequest
    ): Response<ApiResponse<UserWrapper>>

    @POST("api.php?endpoint=change_password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("api.php?endpoint=delete_account")
    suspend fun deleteAccount(): Response<ApiResponse<Map<String, Any>>>

    // ====================================================
    // ORDERS ENDPOINTS
    // ====================================================

    @GET("api.php?endpoint=orders")
    suspend fun getOrders(): Response<ApiResponse<List<OrderDto>>>

    @POST("api.php?endpoint=orders")
    suspend fun createOrder(
        @Body request: CreateOrderRequest
    ): Response<ApiResponse<Map<String, Any>>>
}
