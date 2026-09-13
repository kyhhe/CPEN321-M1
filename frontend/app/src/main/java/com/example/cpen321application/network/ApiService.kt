package com.example.cpen321application.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ServerIpResult(val ip: String)
data class ServerTimeResult(val time: String)
data class NameResult(val firstName: String, val lastName: String)

object ApiService {

    private suspend fun fetchJson(apiBaseUrl: String, path: String): JSONObject? =
        withContext(Dispatchers.IO) {
            val url = "${apiBaseUrl.trimEnd('/')}/$path"
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

    suspend fun getServerIp(apiBaseUrl: String): ServerIpResult? {
        val json = fetchJson(apiBaseUrl, "server-ip") ?: return null
        return ServerIpResult(ip = json.getString("ip"))
    }

    suspend fun getServerTime(apiBaseUrl: String): ServerTimeResult? {
        val json = fetchJson(apiBaseUrl, "server-time") ?: return null
        return ServerTimeResult(time = json.getString("time"))
    }

    suspend fun getMyName(apiBaseUrl: String): NameResult? {
        val json = fetchJson(apiBaseUrl, "my-name") ?: return null
        return NameResult(
            firstName = json.getString("firstName"),
            lastName = json.getString("lastName")
        )
    }
}