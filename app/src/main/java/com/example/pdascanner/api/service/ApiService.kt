package com.example.pdascanner.api.service

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("upload_foto.php")
    suspend fun subirImagen(
        @Part imagen: MultipartBody.Part,
        @Part("qr") qr: RequestBody,
        @Part("nombre") nombre: RequestBody
    ): Response<SimpleResponse> // Usamos retrofit2.Response directamente
}

data class SimpleResponse(
    val success: Boolean,
    val message: String
)