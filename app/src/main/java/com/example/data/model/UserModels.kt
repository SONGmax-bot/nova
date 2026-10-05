package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "status") val status: String? = "active",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "last_login_at") val lastLoginAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthData(
    @Json(name = "user") val user: UserDto,
    @Json(name = "token") val token: String
)

@JsonClass(generateAdapter = true)
data class UserWrapper(
    @Json(name = "user") val user: UserDto
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "phone") val phone: String?,
    @Json(name = "password") val password: String,
    @Json(name = "device_name") val deviceName: String = "Android NOVA App"
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "device_name") val deviceName: String = "Android NOVA App"
)

@JsonClass(generateAdapter = true)
data class ProfileUpdateRequest(
    @Json(name = "name") val name: String,
    @Json(name = "phone") val phone: String?
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    @Json(name = "current_password") val currentPassword: String,
    @Json(name = "new_password") val newPassword: String
)
