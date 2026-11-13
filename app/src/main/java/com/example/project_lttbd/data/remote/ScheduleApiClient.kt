package com.example.project_lttbd.data.remote

import android.util.Log
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ScheduleApiClient {
    private const val BASE_URL = "http://127.0.0.1:8000/"
    private const val LOG_TAG = "MMRA_API_ERROR"

    private val isLocalServer: Boolean
        get() = BASE_URL.startsWith("http://127.0.0.1:") ||
                BASE_URL.startsWith("http://localhost:")

    private val httpClient: OkHttpClient by lazy {
        val useFreshConnection = isLocalServer

        Log.i(
            LOG_TAG,
            "INIT ScheduleApiClient " +
                    "baseUrl=$BASE_URL " +
                    "freshLocalConnection=$useFreshConnection"
        )

        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .followSslRedirects(false)

        if (useFreshConnection) {
            // API local qua USB: không giữ kết nối nhàn rỗi.
            builder.connectionPool(
                ConnectionPool(
                    0,
                    1,
                    TimeUnit.SECONDS
                )
            )
        }

        builder
            .addInterceptor { chain ->
                val originalRequest = chain.request()

                val request = if (useFreshConnection) {
                    originalRequest.newBuilder()
                        .header("Connection", "close")
                        .build()
                } else {
                    originalRequest
                }

                val target =
                    "${request.method} " +
                            "${request.url.scheme}://" +
                            "${request.url.host}:" +
                            "${request.url.port}" +
                            request.url.encodedPath

                val startedAt = System.nanoTime()

                Log.i(
                    LOG_TAG,
                    "START $target"
                )

                try {
                    val response = chain.proceed(request)

                    val elapsedMs = TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - startedAt
                    )

                    Log.i(
                        LOG_TAG,
                        "HTTP ${response.code} $target (${elapsedMs}ms)"
                    )

                    response
                } catch (error: IOException) {
                    Log.e(
                        LOG_TAG,
                        "FAILED $target: " +
                                "${error.javaClass.simpleName}: ${error.message}",
                        error
                    )

                    throw error
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ScheduleApi by lazy {
        retrofit.create(ScheduleApi::class.java)
    }

    val doseApi: DoseApi by lazy {
        retrofit.create(DoseApi::class.java)
    }
}