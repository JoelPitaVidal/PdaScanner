package com.example.pdascanner.api.response

// Esta clase debe representar el JSON que devuelve tu PHP/Servidor
data class ApiResponse(
    val success: Boolean,
    val message: String? = null
)