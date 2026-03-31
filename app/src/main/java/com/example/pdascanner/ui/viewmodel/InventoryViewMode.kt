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

    // 1. FUNCIÓN PRINCIPAL: Procesa todo tras la captura
    fun procesarCaptura(nombreArchivo: String, qr: String, uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            // A. Guardar en Base de Datos Local (Room)
            val pathLocal = uri?.toString() ?: ""
            val nuevaFoto = Foto(
                albaranId = 0,
                nombreFichero = nombreArchivo,
                qrCodigo = qr,
                fecha = System.currentTimeMillis(),
                uri = pathLocal
            )
            repository.guardarFoto(nuevaFoto)

            // B. Intentar subir al servidor si hay URI
            uri?.let {
                val archivo = uriToFile(it)
                if (archivo != null) {
                    subirFotoServidor(archivo, qr, nombreArchivo)
                }
            }
        }
    }

    // 2. LÓGICA DE RED (API)
    private suspend fun subirFotoServidor(archivo: File, qr: String, nombre: String) {
        try {
            val requestFile = archivo.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("foto", archivo.name, requestFile)
            val qrBody = qr.toRequestBody("text/plain".toMediaTypeOrNull())
            val nombreBody = nombre.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = RetrofitClient.instance.subirImagen(body, qrBody, nombreBody)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("API", "Enviado con éxito: ${response.body()?.message}")
            }
        } catch (e: Exception) {
            Log.e("API", "Fallo en la sincronización: ${e.message}")
        }
    }

    // 3. UTILIDAD: Convierte Uri a File (Necesario para Retrofit)
    private fun uriToFile(uri: Uri): File? {
        return try {
            val context = getApplication<Application>().applicationContext
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "upload_temp.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    fun buscarAlbaranPorTransporte(at: String, callback: (Albaran?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val resultado = repository.obtenerAlbaranPorTransporte(at)
            withContext(Dispatchers.Main) { callback(resultado) }
        }
    }

    // Dentro de InventoryViewModel.kt
    sealed class ScanState {
        object Idle : ScanState()
        object Buscando : ScanState()
        data class Valido(val codigo: String) : ScanState()
        data class Error(val mensaje: String) : ScanState()
        data class Guardado(val nombre: String) : ScanState()
    }

    // En el ViewModel añade:
    val estadoEscaneo = MutableLiveData<ScanState>(ScanState.Idle)

    fun procesarCodigo(codigo: String) {
        val limpio = codigo.trim().uppercase()
        estadoEscaneo.postValue(ScanState.Buscando)

        when {
            limpio.startsWith("AT") -> {
                buscarAlbaranPorTransporte(limpio) { albaran ->
                    if (albaran != null) {
                        estadoEscaneo.postValue(ScanState.Valido(albaran.codigoCliente))
                    } else {
                        estadoEscaneo.postValue(ScanState.Error("SIN ASOCIACIÓN: $limpio"))
                    }
                }
            }
            limpio.startsWith("AC") -> {
                estadoEscaneo.postValue(ScanState.Valido(limpio))
            }
            else -> {
                estadoEscaneo.postValue(ScanState.Error("QR NO VÁLIDO (AT/AC)"))
            }
        }
    }

}