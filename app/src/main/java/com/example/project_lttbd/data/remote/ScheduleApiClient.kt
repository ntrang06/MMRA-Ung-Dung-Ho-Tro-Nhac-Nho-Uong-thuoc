
package com.example.project_lttbd.data.remote

import android.util.Log
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ScheduleApiClient {

    private val baseUrl: String
        get() = ApiConfig.BASE_URL

    private const val LOG_TAG = "MMRA_API_ERROR"

    private val isLocalServer: Boolean
        get() = baseUrl.startsWith("http://127.0.0.1:") ||
                baseUrl.startsWith("http://localhost:")

    private val httpClient: OkHttpClient by lazy {
        val useFreshConnection = isLocalServer

        Log.i(
            LOG_TAG,
            "INIT ScheduleApiClient " +
                    "baseUrl=$baseUrl " +
                    "freshLocalConnection=$useFreshConnection"
        )

        val builder = OkHttpClient.Builder()
            .connectTimeout(
                ApiConfig.CONNECT_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            )
            .readTimeout(
                ApiConfig.READ_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            )
            .writeTimeout(
                ApiConfig.WRITE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            )
            .callTimeout(
                ApiConfig.CALL_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            )
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .followSslRedirects(false)

        if (useFreshConnection) {
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
                    val result = chain.proceed(request)

                    val elapsedMs =
                        TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - startedAt
                        )

                    Log.i(
                        LOG_TAG,
                        "HTTP ${result.code} $target (${elapsedMs}ms)"
                    )

                    result
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
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
    }

    val api: ScheduleApi by lazy {
        retrofit.create(ScheduleApi::class.java)
    }

    val doseApi: DoseApi by lazy {
        retrofit.create(DoseApi::class.java)
    }

    val overviewApi: MedicationOverviewApi by lazy {
        retrofit.create(MedicationOverviewApi::class.java)
    }

    val familyApi: FamilyApi by lazy {
        retrofit.create(FamilyApi::class.java)
    }
}
