package com.example.cpen321application.network

import com.example.cpen321application.BuildConfig.API_BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ServerIpResult(val ip: String)
data class ServerTimeResult(val time: String)
data class NameResult(val firstName: String, val lastName: String)

object ApiService {

    private suspend fun fetchJson(endpoint: String): JSONObject? =
        withContext(Dispatchers.IO) {
            val url = "${API_BASE_URL.trimEnd('/')}/$endpoint"
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5_000
                    readTimeout = 5_000
                }

                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        JSONObject(body)
                    }
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }

    suspend fun getServerIp(): ServerIpResult? {
        val json = fetchJson("server-ip") ?: return null
        return ServerIpResult(ip = json.getString("ip"))
    }

    suspend fun getServerTime(): ServerTimeResult? {
        val json = fetchJson("server-time") ?: return null
        return ServerTimeResult(time = json.getString("time"))
    }

    suspend fun getMyName(): NameResult? {
        val json = fetchJson("my-name") ?: return null
        return NameResult(
            firstName = json.getString("firstName"),
            lastName = json.getString("lastName")
        )
    }
}