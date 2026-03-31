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

    // Estados de la pantalla
    sealed class ScanState {
        object Idle : ScanState()
        object Buscando : ScanState()
        data class Valido(val codigo: String) : ScanState()
        data class Error(val mensaje: String) : ScanState()
        // Ahora Guardado incluye el nombre del archivo y el total de fotos
        data class Guardado(val nombre: String, val totalFotos: Int, val qr: String) : ScanState()
    }

    val estadoEscaneo = MutableLiveData<ScanState>(ScanState.Idle)

    // 1. FUNCIÓN PRINCIPAL: Procesa la captura y el conteo
    fun procesarCaptura(nombreArchivo: String, qr: String, uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // A. GUARDAR LOCALMENTE (Siempre primero)
                val nuevaFoto = Foto(
                    albaranId = 0,
                    nombreFichero = nombreArchivo,
                    qrCodigo = qr,
                    fecha = System.currentTimeMillis(),
                    uri = uri?.toString() ?: "",
                    subida = false
                )

                val idGenerado = repository.guardarFoto(nuevaFoto)

                // B. OBTENER CONTEO ACTUALIZADO
                // Consultamos a la DB cuántas fotos tiene ya este QR
                val totalFotos = repository.getConteoFotos(qr)

                // C. NOTIFICAR A LA UI (Actualizamos el mensaje con el contador)
                estadoEscaneo.postValue(ScanState.Guardado(nombreArchivo, totalFotos, qr))

                // D. INTENTAR SUBIDA AL SERVIDOR
                uri?.let {
                    val archivo = uriToFile(it)
                    if (archivo != null) {
                        val exito = subirFotoServidor(archivo, qr, nombreArchivo)
                        if (exito) {
                            repository.marcarFotoComoSubida(idGenerado.toInt())
                            Log.d("SYNC", "Foto $idGenerado sincronizada correctamente")
                        } else {
                            Log.e("SYNC", "Foto $idGenerado guardada solo en local (Fallo servidor/red)")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PROCESS", "Error procesando captura: ${e.message}")
                estadoEscaneo.postValue(ScanState.Error("Error al guardar: ${e.message}"))
            }
        }
    }

    // 2. LÓGICA DE RED (API)
    private suspend fun subirFotoServidor(archivo: File, qr: String, nombre: String): Boolean {
        return try {
            val requestFile = archivo.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("foto", archivo.name, requestFile)
            val qrBody = qr.toRequestBody("text/plain".toMediaTypeOrNull())
            val nombreBody = nombre.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = RetrofitClient.instance.subirImagen(body, qrBody, nombreBody)

            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            Log.e("API", "Error en subida: ${e.message}")
            false
        }
    }

    // 3. PROCESAR CÓDIGOS QR
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
}