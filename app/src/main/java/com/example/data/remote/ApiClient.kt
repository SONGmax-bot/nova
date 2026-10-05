package com.example.data.remote

import android.content.Context
import com.example.data.local.AppPreferences
import com.example.data.local.SecureTokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(
    private val appPreferences: AppPreferences,
    private val secureTokenManager: SecureTokenManager
) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private var cachedRetrofit: Retrofit? = null
    private var cachedBaseUrl: String? = null
    private var cachedService: NovaApiService? = null

    private fun normalizeBaseUrl(input: String): String {
        var url = input.trim()
        if (url.endsWith("api.php")) {
            url = url.substringBeforeLast("api.php")
        }
        if (!url.endsWith("/")) {
            url = "$url/"
        }
        return url
    }

    private fun createOkHttpClient(): OkHttpClient {
        // Redact Authorization header from logs to prevent leaking token
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            if (message.contains("Authorization:", ignoreCase = true) || message.contains("password", ignoreCase = true)) {
                // Redact
            } else {
                android.util.Log.d("NovaApi", message)
            }
        }.apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(appPreferences, secureTokenManager))
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Synchronized
    fun getService(): NovaApiService {
        val currentBaseUrl = normalizeBaseUrl(appPreferences.apiBaseUrl)

        if (cachedService != null && cachedBaseUrl == currentBaseUrl) {
            return cachedService!!
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        cachedBaseUrl = currentBaseUrl
        cachedRetrofit = retrofit
        val service = retrofit.create(NovaApiService::class.java)
        cachedService = service
        return service
    }

    companion object {
        @Volatile
        private var INSTANCE: ApiClient? = null

        fun getInstance(context: Context): ApiClient {
            return INSTANCE ?: synchronized(this) {
                val appPrefs = AppPreferences(context)
                val tokenManager = SecureTokenManager(context)
                val instance = ApiClient(appPrefs, tokenManager)
                INSTANCE = instance
                instance
            }
        }
    }
}
