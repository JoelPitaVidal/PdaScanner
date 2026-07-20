package com.example.pdascanner.api.response

import com.google.gson.annotations.SerializedName

// Representa a cada operario que devuelve el endpoint /api/usuarios/activos
data class UsuarioActivo(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String
)

// Representa la respuesta de éxito de la API al subir una foto
data class RespuestaSubida(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("archivos") val archivos: List<String>,
    @SerializedName("usuario") val usuario: String
)