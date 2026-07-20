package com.example.pdascanner.api

import com.example.pdascanner.api.response.AlbaranResponse
import com.example.pdascanner.api.response.RespuestaSubida
import com.example.pdascanner.api.response.UsuarioActivo
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz de endpoints para comunicación con API_Fotos.
 */
interface ApiService {

    // ============================================================================
    // ENDPOINT: CONSULTAR ALBARÁN
    // ============================================================================
    @GET("api/consultar-albaran/{qr_codigo}")
    suspend fun consultarAlbaran(
        @Path("qr_codigo") codigo: String
    ): Response<AlbaranResponse>

    // ============================================================================
    // ENDPOINT: OBTENER USUARIOS ACTIVOS
    // ============================================================================
    // CORREGIDO: Unificado para usar tu clase importada 'UsuarioActivo' y evitar duplicados
    @GET("api/usuarios/activos")
    suspend fun obtenerUsuariosActivos(): Response<List<UsuarioActivo>>

    // ============================================================================
    // ENDPOINT: SUBIR FOTO
    // ============================================================================
    // CORREGIDO: Unificado en un solo metodo robusto.
    // Usamos 'x-user-id' en minúsculas (estándar HTTP) y la data class tipada 'RespuestaSubida'.
    @Multipart
    @POST("api/fotos/subir")
    suspend fun subirFotoAlbaran(
        @Query("codigo") codigoAlbaran: String,
        @Header("x-user-id") usuarioId: Int,
        @Part file: MultipartBody.Part
    ): Response<RespuestaSubida>
}