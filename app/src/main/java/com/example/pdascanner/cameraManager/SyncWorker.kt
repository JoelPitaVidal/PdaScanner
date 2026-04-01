package com.example.pdascanner.cameraManager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pdascanner.api.RetrofitClient
import com.example.pdascanner.localdatabase.appdatabase.AppDatabase
import com.example.pdascanner.localdatabase.repository.InventoryRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = InventoryRepository(database.albaranDao(), database.fotoDao())

        val fotosPendientes = repository.obtenerFotosNoSubidas()
        if (fotosPendientes.isEmpty()) return Result.success()

        var algunaFallo = false

        for (foto in fotosPendientes) {
            try {
                // Usamos la función local uriToFile
                val archivo = uriToFile(Uri.parse(foto.uri))

                if (archivo != null) {
                    // Usamos la función local subirAServidor
                    val exito = subirAServidor(archivo, foto.qrCodigo, foto.nombreFichero)

                    if (exito) {
                        repository.marcarFotoComoSubida(foto.id.toInt())
                    } else {
                        algunaFallo = true
                    }
                }
            } catch (e: Exception) {
                algunaFallo = true
            }
        }

        return if (algunaFallo) Result.retry() else Result.success()
    }

    // --- FUNCIONES AUXILIARES COPIADAS DEL VIEWMODEL ---

    private suspend fun subirAServidor(archivo: File, qr: String, nombre: String): Boolean {
        return try {
            val requestFile = archivo.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("foto", archivo.name, requestFile)
            val qrBody = qr.toRequestBody("text/plain".toMediaTypeOrNull())
            val nombreBody = nombre.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = RetrofitClient.instance.subirImagen(body, qrBody, nombreBody)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            false
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = applicationContext.contentResolver.openInputStream(uri)
            val file = File(applicationContext.cacheDir, "sync_temp_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)

            // Reutilizamos la compresión que aprendimos para ahorrar datos
            val bitmap = BitmapFactory.decodeStream(inputStream)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)

            outputStream.flush()
            outputStream.close()
            inputStream?.close()
            file
        } catch (e: Exception) {
            null
        }
    }
}