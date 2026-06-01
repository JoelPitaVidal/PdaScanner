package com.example.pdascanner.api

import com.example.pdascanner.api.response.AlbaranResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    //Se cambia @GET con @Path por @Query para coincidir exactamente con albaranes.py
    @GET("api/consultar-albaran")
    suspend fun consultarAlbaran(
        @Query("qr_codigo") codigo: String
    ): Response<AlbaranResponse>

    //Ajustado a la ruta "/api/subir-foto" y parámetros de fotos.py
    @Multipart
    @POST("api/subir-foto")
    suspend fun subirImagen(
        @Part foto: MultipartBody.Part,
        @Part("qr_codigo") qr: RequestBody,
        @Part("fecha") fecha: RequestBody
    ): Response<com.example.pdascanner.api.response.ApiResponse>
}