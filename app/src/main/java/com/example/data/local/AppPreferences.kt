package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "nova_store_app_settings",
        Context.MODE_PRIVATE
    )

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val userAdapter = moshi.adapter(UserDto::class.java)

    var apiBaseUrl: String
        get() = prefs.getString(KEY_API_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit().putString(KEY_API_BASE_URL, value.trim()).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    fun saveCachedUser(user: UserDto) {
        try {
            val json = userAdapter.toJson(user)
            prefs.edit().putString(KEY_CACHED_USER, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCachedUser(): UserDto? {
        val json = prefs.getString(KEY_CACHED_USER, null) ?: return null
        return try {
            userAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun clearCachedUser() {
        prefs.edit().remove(KEY_CACHED_USER).apply()
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://nova1.ct.ws/nova/api.php"
        const val DEFAULT_API_KEY = "NOVA_APP_KEY_2026_SECURE_98127391"

        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_CACHED_USER = "cached_user_profile"
    }
}
