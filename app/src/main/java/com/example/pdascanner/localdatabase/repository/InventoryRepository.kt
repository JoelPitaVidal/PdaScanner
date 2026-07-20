package com.example.pdascanner.localdatabase.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.pdascanner.localdatabase.Albaran
import com.example.pdascanner.localdatabase.AlbaranDao
import com.example.pdascanner.localdatabase.Foto
import com.example.pdascanner.localdatabase.FotoDao
import com.example.pdascanner.sesionmanager.SesionManager // Importante
import kotlinx.coroutines.flow.first
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.pdascanner.api.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

class InventoryRepository(
    val albaranDao: AlbaranDao,
    private val fotoDao: FotoDao
) {

    val fotosPendientes: LiveData<Int> = fotoDao.observarFotosPendientes().asLiveData()

    // --- LÓGICA DE ALBARANES ---

    suspend fun obtenerAlbaranUniversal(codigo: String): Albaran? = albaranDao.buscarAlbaranUniversal(codigo)

    suspend fun insertarAlbaran(albaran: Albaran): Long = albaranDao.insert(albaran)

    suspend fun obtenerAlbaranPorTransporte(at: String): Albaran? = albaranDao.getAlbaranPorTransporte(at)

    suspend fun buscarAlbaranes(query: String): List<Albaran> = albaranDao.buscarAlbaranes(query)

    // --- LÓGICA DE FOTOS ---

    suspend fun guardarFoto(foto: Foto): Long = fotoDao.insert(foto)

    suspend fun marcarFotoComoSubida(fotoId: Long) = fotoDao.marcarComoSubida(fotoId)

    suspend fun obtenerFotosPendientes(): List<Foto> = fotoDao.obtenerPendientesDeSubida()

    suspend fun getConteoFotos(qr: String): Int = fotoDao.contarFotosPorQr(qr)

    fun observarTodosAlbaranes() = albaranDao.getAllAlbaranes()

    suspend fun obtenerTodosAlbaranesLista(): List<Albaran> = albaranDao.getAllAlbaranes().first()

    // --- SINCRONIZACIÓN CORREGIDA ---

    suspend fun subirFotoPendiente(context: Context, foto: Foto, apiService: ApiService) {
        val userId = SesionManager.getUserId(context) ?: return // Si no hay usuario, abortamos

        try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(Uri.parse(foto.uri))

            inputStream?.use { stream ->
                val bytes = stream.readBytes()
                val requestFile = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", foto.nombreFichero, requestFile)

                // CORRECCIÓN: Se añade el userId que requiere el endpoint
                val response = apiService.subirFotoAlbaran(foto.qrCodigo, userId, body)

                if (response.isSuccessful) {
                    marcarFotoComoSubida(foto.id)
                }
            }
        } catch (e: Exception) {
            Log.e("UploadService", "Error subiendo foto: ${e.message}")
        }
    }

    suspend fun procesarColaDeSubida(context: Context, apiService: ApiService) {
        val userId = SesionManager.getUserId(context) ?: return
        val pendientes = fotoDao.obtenerPendientesDeSubida()

        for (foto in pendientes) {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(Uri.parse(foto.uri))

                inputStream?.use { stream ->
                    val bytes = stream.readBytes()
                    val requestFile = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("file", foto.nombreFichero, requestFile)

                    // CORRECCIÓN: Se añade el userId que requiere el endpoint
                    val response = apiService.subirFotoAlbaran(foto.qrCodigo, userId, body)

                    if (response.isSuccessful) {
                        fotoDao.marcarFotoComoSubida(foto.id)
                        Log.d("REPO_SYNC", "Foto ${foto.nombreFichero} subida con éxito.")
                    }
                }
            } catch (e: Exception) {
                Log.e("REPO_SYNC", "Excepción al subir ${foto.nombreFichero}: ${e.message}")
            }
        }
    }
}