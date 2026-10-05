package com.example.data.remote

import com.example.data.local.AppPreferences
import com.example.data.local.SecureTokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val appPreferences: AppPreferences,
    private val secureTokenManager: SecureTokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        // 1. App API Key identification
        val apiKey = appPreferences.apiKey.ifBlank { AppPreferences.DEFAULT_API_KEY }
        builder.header("X-API-Key", apiKey)
        builder.header("Accept", "application/json")

        // 2. User Session Token (if authenticated)
        val userToken = secureTokenManager.getUserToken()
        if (!userToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $userToken")
        }

        val request = builder.build()
        return chain.proceed(request)
    }
}
