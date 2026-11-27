
package com.example.project_lttbd.data.remote

import com.example.project_lttbd.BuildConfig

object ApiConfig {

    val BASE_URL: String
        get() = BuildConfig.API_BASE_URL

    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 90L
    const val WRITE_TIMEOUT_SECONDS = 30L
    const val CALL_TIMEOUT_SECONDS = 120L
}
