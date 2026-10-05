package com.example.data.repository

import com.example.data.local.AppPreferences
import com.example.data.local.SecureTokenManager
import com.example.data.model.ChangePasswordRequest
import com.example.data.model.LoginRequest
import com.example.data.model.ProfileUpdateRequest
import com.example.data.model.RegisterRequest
import com.example.data.model.UserDto
import com.example.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T, val message: String? = null) : AuthResult<T>()
    data class Error(val message: String, val code: Int? = null) : AuthResult<Nothing>()
}

class AuthRepository(
    private val apiClient: ApiClient,
    private val secureTokenManager: SecureTokenManager,
    private val appPreferences: AppPreferences
) {

    private val _currentUser = MutableStateFlow<UserDto?>(appPreferences.getCachedUser())
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(secureTokenManager.hasUserToken())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    suspend fun register(
        name: String,
        email: String,
        phone: String?,
        password: String
    ): AuthResult<UserDto> {
        return try {
            val response = apiClient.getService().register(
                RegisterRequest(
                    name = name.trim(),
                    email = email.trim(),
                    phone = phone?.trim()?.ifBlank { null },
                    password = password
                )
            )

            if (response.isSuccessful && response.body()?.ok == true) {
                val authData = response.body()!!.data
                if (authData != null) {
                    secureTokenManager.saveUserToken(authData.token)
                    appPreferences.saveCachedUser(authData.user)
                    _currentUser.value = authData.user
                    _isLoggedIn.value = true
                    AuthResult.Success(authData.user, response.body()!!.message ?: "تم إنشاء الحساب بنجاح")
                } else {
                    AuthResult.Error("فشل في استلام بيانات التوثيق من الخادم")
                }
            } else {
                val errorMsg = response.body()?.error
                    ?: response.body()?.message
                    ?: "خطأ في التسجيل (${response.code()})"
                AuthResult.Error(errorMsg, response.code())
            }
        } catch (e: Exception) {
            AuthResult.Error("تعذر الاتصال بالخادم: ${e.localizedMessage ?: "تحقق من اتصال الإنترنت"}")
        }
    }

    suspend fun login(email: String, password: String): AuthResult<UserDto> {
        return try {
            val response = apiClient.getService().login(
                LoginRequest(
                    email = email.trim(),
                    password = password
                )
            )

            if (response.isSuccessful && response.body()?.ok == true) {
                val authData = response.body()!!.data
                if (authData != null) {
                    secureTokenManager.saveUserToken(authData.token)
                    appPreferences.saveCachedUser(authData.user)
                    _currentUser.value = authData.user
                    _isLoggedIn.value = true
                    AuthResult.Success(authData.user, response.body()!!.message ?: "تم تسجيل الدخول بنجاح")
                } else {
                    AuthResult.Error("استجابة غير متوقعة من الخادم")
                }
            } else {
                val errorMsg = response.body()?.error
                    ?: response.body()?.message
                    ?: "بيانات الدخول غير صحيحة (${response.code()})"
                AuthResult.Error(errorMsg, response.code())
            }
        } catch (e: Exception) {
            AuthResult.Error("تعذر الاتصال بالخادم: ${e.localizedMessage ?: "تحقق من اتصال الإنترنت"}")
        }
    }

    suspend fun refreshProfile(): AuthResult<UserDto> {
        if (!secureTokenManager.hasUserToken()) {
            return AuthResult.Error("المستخدم غير مسجل الدخول")
        }

        return try {
            val response = apiClient.getService().getMe()
            if (response.isSuccessful && response.body()?.ok == true) {
                val user = response.body()!!.data?.user
                if (user != null) {
                    appPreferences.saveCachedUser(user)
                    _currentUser.value = user
                    _isLoggedIn.value = true
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("بيانات المستخدم فارغة")
                }
            } else {
                if (response.code() == 401 || response.code() == 403) {
                    // Token expired or invalid
                    logoutLocally()
                    AuthResult.Error("انتهت صلاحية الجلسة، يرجى تسجيل الدخول مجدداً", response.code())
                } else {
                    AuthResult.Error(response.body()?.error ?: "فشل تحديث البيانات")
                }
            }
        } catch (e: Exception) {
            // Keep cached user if offline
            val cached = appPreferences.getCachedUser()
            if (cached != null) {
                _currentUser.value = cached
                AuthResult.Success(cached)
            } else {
                AuthResult.Error("تعذر الاتصال: ${e.localizedMessage}")
            }
        }
    }

    suspend fun updateProfile(name: String, phone: String?): AuthResult<UserDto> {
        return try {
            val response = apiClient.getService().updateProfile(
                ProfileUpdateRequest(name = name.trim(), phone = phone?.trim()?.ifBlank { null })
            )

            if (response.isSuccessful && response.body()?.ok == true) {
                val user = response.body()!!.data?.user
                if (user != null) {
                    appPreferences.saveCachedUser(user)
                    _currentUser.value = user
                    AuthResult.Success(user, response.body()!!.message ?: "تم تحديث البيانات بنجاح")
                } else {
                    AuthResult.Error("لم يتم استلام البيانات المحدثة")
                }
            } else {
                AuthResult.Error(response.body()?.error ?: "فشل في تحديث الحساب")
            }
        } catch (e: Exception) {
            AuthResult.Error("خطأ في الاتصال: ${e.localizedMessage}")
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult<Unit> {
        return try {
            val response = apiClient.getService().changePassword(
                ChangePasswordRequest(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
            )

            if (response.isSuccessful && response.body()?.ok == true) {
                AuthResult.Success(Unit, response.body()!!.message ?: "تم تغيير كلمة المرور بنجاح")
            } else {
                AuthResult.Error(response.body()?.error ?: "فشل تغيير كلمة المرور")
            }
        } catch (e: Exception) {
            AuthResult.Error("خطأ في الاتصال: ${e.localizedMessage}")
        }
    }

    suspend fun logout(): AuthResult<Unit> {
        try {
            apiClient.getService().logout()
        } catch (_: Exception) {
            // Even if server call fails or is offline, clear locally
        }
        logoutLocally()
        return AuthResult.Success(Unit, "تم تسجيل الخروج بنجاح")
    }

    suspend fun deleteAccount(): AuthResult<Unit> {
        return try {
            val response = apiClient.getService().deleteAccount()
            if (response.isSuccessful && response.body()?.ok == true) {
                logoutLocally()
                AuthResult.Success(Unit, response.body()!!.message ?: "تم حذف الحساب بنجاح")
            } else {
                AuthResult.Error(response.body()?.error ?: "فشل حذف الحساب")
            }
        } catch (e: Exception) {
            AuthResult.Error("خطأ في الاتصال: ${e.localizedMessage}")
        }
    }

    fun logoutLocally() {
        secureTokenManager.clearUserToken()
        appPreferences.clearCachedUser()
        _currentUser.value = null
        _isLoggedIn.value = false
    }
}
