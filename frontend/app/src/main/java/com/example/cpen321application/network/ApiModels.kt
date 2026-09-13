package com.example.cpen321application.network

// Data class to match the JSON returns of the backend
data class ServerIpResponse(
    val ip: String
)

data class ServerTimeResponse(
    val time: String
)

data class NameResponse(
    val firstName: String,
    val lastName: String
)