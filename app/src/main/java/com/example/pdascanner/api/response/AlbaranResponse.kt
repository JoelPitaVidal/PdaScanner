package com.example.pdascanner.api.response

data class AlbaranResponse(
    val success: Boolean,
    val message: String?,
    val cliente: String?,
    val estado: String?,
    val fecha_creacion: String?,
    val total_bultos: Int?
)