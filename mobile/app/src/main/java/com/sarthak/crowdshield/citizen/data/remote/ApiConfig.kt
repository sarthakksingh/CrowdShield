package com.sarthak.crowdshield.citizen.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {
    // 10.0.2.2 is the standard loopback alias for the host machine in the Android Emulator.
    // Replace with local Wi-Fi IP (e.g. http://192.168.1.X:8080/) when deploying to a physical device.
    const val DEFAULT_BASE_URL: String = "http://10.0.2.2:8080/"

    private var currentBaseUrl: String = DEFAULT_BASE_URL
    private var cachedApi: CrowdShieldApi? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun getApi(url: String = currentBaseUrl): CrowdShieldApi {
        if (cachedApi == null || currentBaseUrl != url) {
            currentBaseUrl = if (url.endsWith("/")) url else "$url/"
            cachedApi = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CrowdShieldApi::class.java)
        }
        return cachedApi!!
    }
}
