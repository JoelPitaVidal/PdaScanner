package com.example.pdascanner.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pdascanner.api.RetrofitClient
import com.example.pdascanner.localdatabase.Albaran
import com.example.pdascanner.localdatabase.Foto
import com.example.pdascanner.localdatabase.appdatabase.AppDatabase
import com.example.pdascanner.localdatabase.repository.InventoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InventoryRepository = AppDatabase.getDatabase(application).let {
        InventoryRepository(it.albaranDao(), it.fotoDao())
    }

    sealed class ScanState {
        object Idle : ScanState()
        object Buscando : ScanState()
        data class Valido(val codigo: String) : ScanState()
        data class Error(val mensaje: String) : ScanState()
        data class Guardado(val nombre: String, val totalFotos: Int, val qr: String) : ScanState()
    }

    val estadoEscaneo = MutableLiveData<ScanState>(ScanState.Idle)

    private fun tieneEspacioSuficiente(): Boolean {
        val path = getApplication<Application>().filesDir
        val espacioLibreBytes = path.freeSpace
        val mbLibres = espacioLibreBytes / (1024 * 1024)

        Log.d("STORAGE", "Espacio disponible: $mbLibres MB")

        // Retornamos falso si queda menos de 100MB por seguridad
        return mbLibres > 100
    }

    fun procesarCaptura(nombreArchivo: String, qr: String, uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. COMPROBACIÓN CRÍTICA DE ESPACIO
            if (!tieneEspacioSuficiente()) {
                estadoEscaneo.postValue(ScanState.Error("MEMORIA LLENA: Libera espacio en la PDA"))
                return@launch
            }

            try {
                // A. GUARDAR LOCALMENTE
                val nuevaFoto = Foto(
                    albaranId = 0,
                    nombreFichero = nombreArchivo,
                    qrCodigo = qr,
                    fecha = System.currentTimeMillis(),
                    uri = uri?.toString() ?: "",
                    subida = false
                )

                val idGenerado = repository.guardarFoto(nuevaFoto)
                val totalFotos = repository.getConteoFotos(qr)

                estadoEscaneo.postValue(ScanState.Guardado(nombreArchivo, totalFotos, qr))

                // B. INTENTAR SUBIDA AL SERVIDOR
                uri?.let {
                    val archivo = uriToFile(it)
                    if (archivo != null) {
                        val exito = subirFotoServidor(archivo, qr, nombreArchivo)
                        if (exito) {
                            repository.marcarFotoComoSubida(idGenerado.toInt())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PROCESS", "Error: ${e.message}")
                estadoEscaneo.postValue(ScanState.Error("Error de escritura: ${e.message}"))
            }
        }
    }

    // ... (Resto de funciones: subirFotoServidor, procesarCodigo, etc. se mantienen igual)

    private suspend fun subirFotoServidor(archivo: File, qr: String, nombre: String): Boolean {
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

    fun procesarCodigo(codigo: String) {
        val limpio = codigo.trim().uppercase()
        estadoEscaneo.postValue(ScanState.Buscando)
        when {
            limpio.startsWith("AT") -> {
                buscarAlbaranPorTransporte(limpio) { albaran ->
                    if (albaran != null) estadoEscaneo.postValue(ScanState.Valido(albaran.codigoCliente))
                    else estadoEscaneo.postValue(ScanState.Error("SIN ASOCIACIÓN: $limpio"))
                }
            }
            limpio.startsWith("AC") -> estadoEscaneo.postValue(ScanState.Valido(limpio))
            else -> estadoEscaneo.postValue(ScanState.Error("QR NO VÁLIDO (AT/AC)"))
        }
    }

    private fun buscarAlbaranPorTransporte(at: String, callback: (Albaran?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val resultado = repository.obtenerAlbaranPorTransporte(at)
            withContext(Dispatchers.Main) { callback(resultado) }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val context = getApplication<Application>().applicationContext
            val inputStream = context.contentResolver.openInputStream(uri)

            // 1. Decodificamos el InputStream a un objeto Bitmap
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // 2. Creamos el archivo de destino en la caché
            val file = File(context.cacheDir, "upload_temp.jpg")
            val outputStream = FileOutputStream(file)

            // 3. COMPRESIÓN MÁGICA:
            // Convertimos el bitmap a JPEG con calidad 75 (0-100)
            // Esto reduce drásticamente el peso manteniendo la legibilidad de textos
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, outputStream)

            outputStream.flush()
            outputStream.close()

            // Liberamos memoria del bitmap
            bitmap.recycle()

            file
        } catch (e: Exception) {
            Log.e("STORAGE", "Error comprimiendo imagen: ${e.message}")
            null
        }
    }
}